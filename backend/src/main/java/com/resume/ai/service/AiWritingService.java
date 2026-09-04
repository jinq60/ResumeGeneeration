package com.resume.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.ai.config.AiPromptTemplates;
import com.resume.ai.config.AiProperties;
import com.resume.ai.dto.AiChatRequest;
import com.resume.ai.dto.AiChatResponse;
import com.resume.ai.dto.ResumeAiWriteRequest;
import com.resume.ai.dto.ResumeAiWriteResponse;
import com.resume.ai.entity.AiCallLog;
import com.resume.ai.mapper.AiCallLogMapper;
import com.resume.ai.provider.LlmProvider;
import com.resume.ai.provider.ProviderRouter;
import com.resume.ai.util.AiCallLogDefaults;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.common.service.AuditLogService;
import com.resume.resume.entity.Resume;
import com.resume.resume.service.ResumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import reactor.core.publisher.Flux;

/**
 * 行内 AI 写作服务：对简历指定字段执行生成/润色/缩短/扩写/翻译。
 * <p>
 * 同步执行（用户等待结果），复用多厂商 LLM 路由与调用审计；
 * 每用户并发上限防止接口被刷爆 LLM 配额；按日配额区分游客与登录用户。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiWritingService {

    public static final String FEATURE_KEY = "resume-writing";

    private static final int MAX_TEXT_LENGTH = 2000;
    private static final int MAX_PROMPT_TOKENS = 1500;
    // api-spec §7.10 字段长度上限
    private static final int MAX_SECTION_TYPE_LENGTH = 32;
    private static final int MAX_FIELD_LENGTH = 64;
    private static final int MAX_TARGET_LANG_LENGTH = 16;

    private static final Set<String> ACTIONS = Set.of("generate", "polish", "shorten", "expand", "translate");

    /**
     * 各 Section 类型可写作的字段白名单。
     */
    private static final Map<String, Set<String>> FIELD_WHITELIST = Map.of(
            "profile", Set.of("name", "targetPosition", "expectedSalary", "availability"),
            "education", Set.of("school", "major", "degree", "honors", "courses"),
            "work", Set.of("company", "position", "description", "achievements"),
            "project", Set.of("name", "role", "description", "achievements", "background", "responsibility"),
            "skill", Set.of("items"),
            "introduction", Set.of("content")
    );

    private final ResumeService resumeService;
    private final ProviderRouter providerRouter;
    private final AiPromptTemplates promptTemplates;
    private final AiCallLogMapper aiCallLogMapper;
    private final AuditLogService auditLogService;
    private final AiDailyQuotaService aiDailyQuotaService;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    /** 每用户进行中请求计数（内存实现，单实例部署适用） */
    private final Map<String, AtomicInteger> inFlight = new ConcurrentHashMap<>();

    /**
     * 执行行内 AI 写作。
     * <p>
     * 顺序：参数校验（归属 / 白名单 / 动作 / 原文）→ 并发槽位 → provider 解析 →
     * 扣减按日配额 → 调用 LLM；调用失败时退还配额，避免"先扣不退"。
     * </p>
     */
    public ResumeAiWriteResponse write(String userId, boolean guest, String resumeId,
                                       ResumeAiWriteRequest request) {
        Resume resume = resumeService.getResumeEntity(userId, resumeId);
        validateRequest(request);

        String originalText = StringUtils.defaultString(request.getOriginalText());
        validateOriginalText(request, originalText);

        if (inFlight.size() > 10000) {
            log.warn("AiWriting inFlight map too large ({}), evicting idle entries", inFlight.size());
            inFlight.entrySet().removeIf(e -> e.getValue().get() <= 0);
            if (inFlight.size() > 10000) {
                log.warn("AiWriting inFlight still too large after eviction ({}), rejecting", inFlight.size());
                throw new BusinessException(ResultCode.AI_CONCURRENT_LIMIT_EXCEEDED, "系统繁忙，请稍后再试。");
            }
        }
        AtomicInteger counter = inFlight.computeIfAbsent(userId, k -> new AtomicInteger());
        if (counter.incrementAndGet() > aiProperties.getRateLimit().getMaxConcurrentPerUser()) {
            counter.decrementAndGet();
            throw new BusinessException(ResultCode.AI_CONCURRENT_LIMIT_EXCEEDED,
                    "同时进行的 AI 请求过多，请稍后再试。");
        }
        try {
            // provider 解析放在配额扣减之前，解析失败不消耗配额
            LlmProvider provider = providerRouter.resolve(FEATURE_KEY);
            String model = providerRouter.resolveModel(FEATURE_KEY);
            log.info("AI writing chat -> provider={}, model={}, resumeId={}, section={}, field={}, action={}",
                    provider.getProviderName(), model, resume.getId(),
                    request.getSectionType(), request.getField(), request.getAction());
            String prompt = buildPrompt(resume, request, originalText);
            AiProperties.FeatureConfig featureConfig = providerRouter.getFeatureConfig(FEATURE_KEY);
            AiChatRequest aiRequest = buildChatRequest(model, prompt, featureConfig);

            // 所有校验与解析完成后、发起 LLM 调用前才扣减配额
            String quotaDate = consumeQuota(userId, guest);
            try {
                return doWrite(resume, request, originalText, provider, model, prompt, aiRequest);
            } catch (Exception e) {
                // LLM 调用失败退还本次配额（used_count 下限保护为 0），按消费时的 quotaDate 精准退款
                aiDailyQuotaService.refund(userId, FEATURE_KEY, quotaDate);
                throw e;
            }
        } finally {
            releaseInFlight(userId, counter);
        }
    }

    /**
     * Stream generated content as text deltas. The same validation and quota
     * rules as the synchronous endpoint apply: quota is consumed only after all
     * validation / provider resolution, right before the LLM call, and refunded
     * when the stream fails.
     */
    public Flux<String> stream(String userId, boolean guest, String resumeId,
                               ResumeAiWriteRequest request) {
        Resume resume = resumeService.getResumeEntity(userId, resumeId);
        validateRequest(request);

        String originalText = StringUtils.defaultString(request.getOriginalText());
        validateOriginalText(request, originalText);

        if (inFlight.size() > 10000) {
            log.warn("AiWriting stream inFlight too large, evicting idle", inFlight.size());
            inFlight.entrySet().removeIf(e -> e.getValue().get() <= 0);
            if (inFlight.size() > 10000) {
                throw new BusinessException(ResultCode.AI_CONCURRENT_LIMIT_EXCEEDED, "系统繁忙，请稍后再试。");
            }
        }
        AtomicInteger counter = inFlight.computeIfAbsent(userId, k -> new AtomicInteger());
        if (counter.incrementAndGet() > aiProperties.getRateLimit().getMaxConcurrentPerUser()) {
            counter.decrementAndGet();
            throw new BusinessException(ResultCode.AI_CONCURRENT_LIMIT_EXCEEDED,
                    "同时进行的 AI 请求过多，请稍后再试。");
        }

        final java.util.concurrent.atomic.AtomicReference<String> quotaDateRef = new java.util.concurrent.atomic.AtomicReference<>();
        boolean quotaConsumed = false;
        try {
            LlmProvider provider = providerRouter.resolve(FEATURE_KEY);
            String model = providerRouter.resolveModel(FEATURE_KEY);
            String prompt = buildPrompt(resume, request, originalText);
            AiProperties.FeatureConfig featureConfig = providerRouter.getFeatureConfig(FEATURE_KEY);
            AiChatRequest aiRequest = buildChatRequest(model, prompt, featureConfig);
            AiCallLog callLog = new AiCallLog();
            callLog.setUserId(resume.getUserId());
            callLog.setFeatureKey(FEATURE_KEY);
            callLog.setProviderName(provider.getProviderName());
            callLog.setModelName(model);
            callLog.setRequestHash(DigestUtils.md5DigestAsHex(prompt.getBytes(StandardCharsets.UTF_8)));
            callLog.setCreatedAt(LocalDateTime.now());
            long start = System.currentTimeMillis();
            AtomicBoolean finalized = new AtomicBoolean(false);

            // 所有校验与解析完成后、发起 LLM 调用前才扣减配额
            String quotaDate = consumeQuota(userId, guest);
            quotaDateRef.set(quotaDate);
            quotaConsumed = true;

            return provider.stream(aiRequest)
                    .filter(StringUtils::isNotBlank)
                    .doOnComplete(() -> finishStream(callLog, resume, request, start, true, finalized, quotaDateRef.get()))
                    .doOnError(error -> finishStream(callLog, resume, request, start, false, finalized, quotaDateRef.get()))
                    .doFinally(signal -> {
                        if (signal == reactor.core.publisher.SignalType.CANCEL) {
                            finishStream(callLog, resume, request, start, false, finalized, quotaDateRef.get());
                        }
                        releaseInFlight(userId, counter);
                    });
        } catch (RuntimeException e) {
            if (quotaConsumed) {
                try {
                    String qd = quotaDateRef.get();
                    if (qd != null) {
                        aiDailyQuotaService.refund(resume.getUserId(), FEATURE_KEY, qd);
                    } else {
                        aiDailyQuotaService.refund(resume.getUserId(), FEATURE_KEY);
                    }
                } catch (Exception ignore) {}
            }
            releaseInFlight(userId, counter);
            throw e;
        }
    }

    private ResumeAiWriteResponse doWrite(Resume resume, ResumeAiWriteRequest request, String originalText,
                                          LlmProvider provider, String model,
                                          String prompt, AiChatRequest aiRequest) {
        long start = System.currentTimeMillis();
        AiCallLog callLog = new AiCallLog();
        callLog.setUserId(resume.getUserId());
        callLog.setFeatureKey(FEATURE_KEY);
        callLog.setCreatedAt(LocalDateTime.now());

        try {
            AiChatResponse response = provider.chat(aiRequest);

            callLog.setProviderName(provider.getProviderName());
            callLog.setModelName(model);
            callLog.setRequestHash(DigestUtils.md5DigestAsHex(prompt.getBytes(StandardCharsets.UTF_8)));
            callLog.setPromptTokens(response.getPromptTokens());
            callLog.setCompletionTokens(response.getCompletionTokens());
            callLog.setTotalTokens(response.getTotalTokens());
            callLog.setLatencyMs(response.getLatencyMs());
            callLog.setSuccess(response.isSuccess());

            if (!response.isSuccess()) {
                String errorMsg = response.getErrorMsg();
                callLog.setErrorMsg(errorMsg);
                throw new BusinessException(ResultCode.AI_MODEL_CALL_FAILED,
                        "AI 写作失败：" + StringUtils.defaultString(errorMsg, "模型调用异常"));
            }

            String content = StringUtils.defaultString(response.getContent()).trim();
            if (content.isEmpty()) {
                callLog.setErrorMsg("empty content");
                throw new BusinessException(ResultCode.AI_RESPONSE_PARSE_FAILED, "AI 未返回内容，请重试。");
            }

            try {
                auditLogService.record(resume.getUserId(), "ai_write", resume.getId(),
                        "section=" + request.getSectionType() + ", field=" + request.getField() + ", action=" + request.getAction());
            } catch (Exception auditEx) {
                log.warn("AI writing audit failed, ignore for billing: resumeId={}", resume.getId(), auditEx);
            }
            return new ResumeAiWriteResponse(content);
        } catch (Exception e) {
            callLog.setSuccess(false);
            callLog.setErrorMsg(e.getMessage());
            callLog.setLatencyMs(System.currentTimeMillis() - start);
            if (e instanceof BusinessException be) {
                throw be;
            }
            log.error("AI writing failed: resumeId={}", resume.getId(), e);
            throw new BusinessException(ResultCode.AI_MODEL_CALL_FAILED, "AI 写作失败，请稍后重试。");
        } finally {
            AiCallLogDefaults.fillRequiredColumns(callLog, provider.getProviderName());
            try {
                aiCallLogMapper.insert(callLog);
            } catch (Exception logEx) {
                log.warn("Insert AiCallLog failed: {}", logEx.getMessage());
            }
        }
    }

    /**
     * 构建 LLM 请求（在配额扣减前完成解析与构建）。
     */
    private AiChatRequest buildChatRequest(String model, String prompt,
                                           AiProperties.FeatureConfig featureConfig) {
        return AiChatRequest.builder()
                .model(model)
                .userPrompt(prompt)
                .temperature(0.4)
                .maxTokens(MAX_PROMPT_TOKENS)
                .timeout(featureConfig.getTimeout())
                .retry(featureConfig.getRetry())
                .build();
    }

    /**
     * 释放并发计数，并在计数归零时清理内存条目，避免 inFlight Map 无限增长。
     */
    private void releaseInFlight(String userId, AtomicInteger counter) {
        int v = counter.decrementAndGet();
        if (v <= 0) {
            if (v < 0) {
                counter.set(0);
            }
            inFlight.remove(userId, counter);
        }
    }

    /**
     * 按日配额扣减：游客与登录用户使用不同上限，超限直接拒绝。
     * 仅在所有校验与 provider 解析完成、即将发起 LLM 调用前调用。
     *
     * @return 本次消费对应的 quotaDate，失败时由调用方透传至 refund 精准回退
     */
    private String consumeQuota(String userId, boolean guest) {
        int dailyLimit = guest
                ? aiProperties.getDailyQuota().getGuest()
                : aiProperties.getDailyQuota().getUser();
        return aiDailyQuotaService.consume(userId, FEATURE_KEY, dailyLimit);
    }

    private void validateRequest(ResumeAiWriteRequest request) {
        // api-spec §7.10 字段长度上限
        if (StringUtils.length(request.getSectionType()) > MAX_SECTION_TYPE_LENGTH) {
            throw new BusinessException(ResultCode.AI_WRITING_FIELD_INVALID,
                    "模块类型长度不能超过 " + MAX_SECTION_TYPE_LENGTH + " 字符。");
        }
        if (StringUtils.length(request.getField()) > MAX_FIELD_LENGTH) {
            throw new BusinessException(ResultCode.AI_WRITING_FIELD_INVALID,
                    "字段名长度不能超过 " + MAX_FIELD_LENGTH + " 字符。");
        }
        if (!FIELD_WHITELIST.containsKey(request.getSectionType())
                || !FIELD_WHITELIST.get(request.getSectionType()).contains(request.getField())) {
            throw new BusinessException(ResultCode.AI_WRITING_FIELD_INVALID, "该字段暂不支持 AI 写作。");
        }
        if (!ACTIONS.contains(request.getAction())) {
            throw new BusinessException(ResultCode.AI_WRITING_FIELD_INVALID, "不支持的 AI 写作动作。");
        }
        if ("translate".equals(request.getAction())) {
            if (StringUtils.isBlank(request.getTargetLang())) {
                throw new BusinessException(ResultCode.AI_WRITING_FIELD_INVALID, "翻译需要指定目标语言。");
            }
            if (request.getTargetLang().length() > MAX_TARGET_LANG_LENGTH) {
                throw new BusinessException(ResultCode.AI_WRITING_FIELD_INVALID,
                        "目标语言长度不能超过 " + MAX_TARGET_LANG_LENGTH + " 字符。");
            }
        }
    }

    private void validateOriginalText(ResumeAiWriteRequest request, String originalText) {
        if (originalText.length() > MAX_TEXT_LENGTH) {
            throw new BusinessException(ResultCode.AI_WRITING_CONTENT_TOO_LONG,
                    "原文过长，AI 写作单次最多支持 " + MAX_TEXT_LENGTH + " 字符。");
        }
        if (!"generate".equals(request.getAction()) && StringUtils.isBlank(originalText)) {
            throw new BusinessException(ResultCode.AI_WRITING_FIELD_INVALID,
                    "请先填写内容，再进行 AI 改写。");
        }
    }

    private String buildPrompt(Resume resume, ResumeAiWriteRequest request, String originalText) {
        String resumeContent = toJson(resume.getSections());
        return promptTemplates.render(FEATURE_KEY, Map.of(
                "resumeContent", resumeContent,
                "jobDescription", StringUtils.defaultString(resume.getTargetPosition()),
                "sectionType", request.getSectionType(),
                "field", request.getField(),
                "action", request.getAction(),
                "originalText", originalText,
                "targetLang", StringUtils.defaultString(request.getTargetLang())
        ));
    }

    private void finishStream(AiCallLog callLog, Resume resume, ResumeAiWriteRequest request,
                              long start, boolean success, AtomicBoolean finalized, String quotaDate) {
        if (!finalized.compareAndSet(false, true)) return;
        callLog.setSuccess(success);
        callLog.setLatencyMs(System.currentTimeMillis() - start);
        if (!success) {
            callLog.setErrorMsg("AI stream failed");
            // 流式调用失败退还本次配额（finalized 保证只退一次），按消费时的 quotaDate 精准退款
            try {
                if (quotaDate != null) {
                    aiDailyQuotaService.refund(resume.getUserId(), FEATURE_KEY, quotaDate);
                } else {
                    aiDailyQuotaService.refund(resume.getUserId(), FEATURE_KEY);
                }
            } catch (Exception ignore) {
                log.warn("Refund quota on stream failure failed", ignore);
            }
        } else {
            auditLogService.record(resume.getUserId(), "ai_write", resume.getId(),
                    "stream=true, section=" + request.getSectionType()
                            + ", field=" + request.getField() + ", action=" + request.getAction());
        }
        try {
            String providerName = callLog.getProviderName();
            AiCallLogDefaults.fillRequiredColumns(callLog, providerName);
            aiCallLogMapper.insert(callLog);
        } catch (Exception logEx) {
            log.warn("Insert streaming AiCallLog failed: {}", logEx.getMessage());
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
