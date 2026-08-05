package com.resume.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.ai.config.AiPromptTemplates;
import com.resume.ai.dto.AiChatRequest;
import com.resume.ai.dto.AiChatResponse;
import com.resume.ai.dto.ResumeAiWriteRequest;
import com.resume.ai.dto.ResumeAiWriteResponse;
import com.resume.ai.entity.AiCallLog;
import com.resume.ai.mapper.AiCallLogMapper;
import com.resume.ai.provider.LlmProvider;
import com.resume.ai.provider.ProviderRouter;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.common.service.AuditLogService;
import com.resume.resume.entity.Resume;
import com.resume.resume.service.ResumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 行内 AI 写作服务：对简历指定字段执行生成/润色/缩短/扩写/翻译。
 * <p>
 * 同步执行（用户等待结果），复用多厂商 LLM 路由与调用审计；
 * 每用户并发上限防止接口被刷爆 LLM 配额。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiWritingService {

    public static final String FEATURE_KEY = "resume-writing";

    private static final int MAX_CONCURRENT_PER_USER = 10;
    private static final int MAX_TEXT_LENGTH = 2000;
    private static final int MAX_PROMPT_TOKENS = 1500;

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
    private final ObjectMapper objectMapper;

    /** 每用户进行中请求计数（内存实现，单实例部署适用） */
    private final Map<String, AtomicInteger> inFlight = new ConcurrentHashMap<>();

    /**
     * 执行行内 AI 写作。
     */
    public ResumeAiWriteResponse write(String userId, String resumeId, ResumeAiWriteRequest request) {
        Resume resume = resumeService.getResumeEntity(userId, resumeId);

        // 白名单校验
        if (!FIELD_WHITELIST.containsKey(request.getSectionType())
                || !FIELD_WHITELIST.get(request.getSectionType()).contains(request.getField())) {
            throw new BusinessException(ResultCode.AI_WRITING_FIELD_INVALID,
                    "该字段暂不支持 AI 写作。");
        }
        if (!ACTIONS.contains(request.getAction())) {
            throw new BusinessException(ResultCode.AI_WRITING_FIELD_INVALID, "不支持的 AI 写作动作。");
        }
        if ("translate".equals(request.getAction()) && StringUtils.isBlank(request.getTargetLang())) {
            throw new BusinessException(ResultCode.AI_WRITING_FIELD_INVALID, "翻译需要指定目标语言。");
        }

        String originalText = StringUtils.defaultString(request.getOriginalText());
        if (originalText.length() > MAX_TEXT_LENGTH) {
            throw new BusinessException(ResultCode.AI_WRITING_CONTENT_TOO_LONG,
                    "原文过长，AI 写作单次最多支持 " + MAX_TEXT_LENGTH + " 字符。");
        }
        if (!"generate".equals(request.getAction()) && StringUtils.isBlank(originalText)) {
            throw new BusinessException(ResultCode.AI_WRITING_FIELD_INVALID,
                    "请先填写内容，再进行 AI 改写。");
        }

        AtomicInteger counter = inFlight.computeIfAbsent(userId, k -> new AtomicInteger());
        if (counter.incrementAndGet() > MAX_CONCURRENT_PER_USER) {
            counter.decrementAndGet();
            throw new BusinessException(ResultCode.AI_CONCURRENT_LIMIT_EXCEEDED,
                    "同时进行的 AI 请求过多，请稍后再试。");
        }
        try {
            return doWrite(resume, request, originalText);
        } finally {
            counter.decrementAndGet();
        }
    }

    private ResumeAiWriteResponse doWrite(Resume resume, ResumeAiWriteRequest request, String originalText) {
        long start = System.currentTimeMillis();
        AiCallLog callLog = new AiCallLog();
        callLog.setUserId(resume.getUserId());
        callLog.setFeatureKey(FEATURE_KEY);
        callLog.setCreatedAt(LocalDateTime.now());

        try {
            LlmProvider provider = providerRouter.resolve(FEATURE_KEY);
            String model = providerRouter.resolveModel(FEATURE_KEY);
            log.info("AI writing chat -> provider={}, model={}, resumeId={}, section={}, field={}, action={}",
                    provider.getProviderName(), model, resume.getId(),
                    request.getSectionType(), request.getField(), request.getAction());

            String resumeContent = toJson(resume.getSections());
            String prompt = promptTemplates.render(FEATURE_KEY, Map.of(
                    "resumeContent", resumeContent,
                    "jobDescription", StringUtils.defaultString(resume.getTargetPosition()),
                    "sectionType", request.getSectionType(),
                    "field", request.getField(),
                    "action", request.getAction(),
                    "originalText", originalText,
                    "targetLang", StringUtils.defaultString(request.getTargetLang())
            ));

            AiChatRequest aiRequest = AiChatRequest.builder()
                    .model(model)
                    .userPrompt(prompt)
                    .temperature(0.4)
                    .maxTokens(MAX_PROMPT_TOKENS)
                    .build();

            AiChatResponse response = provider.chat(aiRequest);

            callLog.setProviderName(provider.getProviderName());
            callLog.setModelName(model);
            callLog.setRequestHash(String.valueOf(prompt.hashCode()));
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

            auditLogService.record(resume.getUserId(), "ai_write", resume.getId(),
                    "section=" + request.getSectionType() + ", field=" + request.getField() + ", action=" + request.getAction());
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
            try {
                aiCallLogMapper.insert(callLog);
            } catch (Exception logEx) {
                log.warn("Insert AiCallLog failed: {}", logEx.getMessage());
            }
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
