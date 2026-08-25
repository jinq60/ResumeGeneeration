package com.resume.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.ai.config.AiPromptTemplates;
import com.resume.ai.config.AiProperties;
import com.resume.ai.dto.AiChatRequest;
import com.resume.ai.dto.GrammarCheckResponse;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AI 简历语法检查：返回可定位到 Section 字段的修改建议。
 * <p>
 * 与 AI 写作一致：按日配额区分游客与登录用户，配额在校验后、调用前扣减，
 * 调用失败退还；每用户并发槽位限制防止接口被刷爆 LLM 配额。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiGrammarService {

    public static final String FEATURE_KEY = "resume-grammar";

    private final ResumeService resumeService;
    private final AiProperties aiProperties;
    private final ProviderRouter providerRouter;
    private final AiPromptTemplates promptTemplates;
    private final AiCallLogMapper aiCallLogMapper;
    private final AuditLogService auditLogService;
    private final AiDailyQuotaService aiDailyQuotaService;
    private final ObjectMapper objectMapper;

    /** 每用户进行中请求计数（内存实现，单实例部署适用） */
    private final Map<String, AtomicInteger> inFlight = new ConcurrentHashMap<>();

    public GrammarCheckResponse check(String userId, boolean guest, String resumeId) {
        Resume resume = resumeService.getResumeEntity(userId, resumeId);
        GrammarCheckResponse fallback = new GrammarCheckResponse();
        fallback.setStatus("unavailable");
        fallback.setModel("unavailable");
        fallback.setMessage("未配置语法检查 AI 供应商。");
        fallback.setIssues(List.of());
        fallback.setCheckedAt(LocalDateTime.now());

        LlmProvider provider;
        String model;
        try {
            AiProperties.FeatureConfig featureConfig = providerRouter.getFeatureConfig(FEATURE_KEY);
            if (featureConfig == null || !hasProviderKey(featureConfig.getProvider())) {
                return fallback;
            }
            provider = providerRouter.resolve(FEATURE_KEY);
            model = providerRouter.resolveModel(FEATURE_KEY);
        } catch (BusinessException ex) {
            if (ex.getErrorCode() == ResultCode.AI_PROVIDER_NOT_CONFIGURED) {
                // 未配置供应商属可用性回退，不消耗配额
                return fallback;
            }
            throw ex;
        }

        // 并发槽位限制（与 AI 写作同一模式）
        AtomicInteger counter = inFlight.computeIfAbsent(userId, k -> new AtomicInteger());
        if (counter.incrementAndGet() > aiProperties.getRateLimit().getMaxConcurrentPerUser()) {
            counter.decrementAndGet();
            throw new BusinessException(ResultCode.AI_CONCURRENT_LIMIT_EXCEEDED,
                    "同时进行的 AI 请求过多，请稍后再试。");
        }

        try {
            long start = System.currentTimeMillis();
            AiCallLog callLog = new AiCallLog();
            callLog.setUserId(userId);
            callLog.setFeatureKey(FEATURE_KEY);
            callLog.setProviderName(provider.getProviderName());
            callLog.setModelName(model);
            callLog.setCreatedAt(LocalDateTime.now());

            try {
                String prompt = promptTemplates.render(FEATURE_KEY, Map.of(
                        "resumeContent", toJson(resume.getSections()),
                        "targetPosition", StringUtils.defaultString(resume.getTargetPosition())
                ));
                callLog.setRequestHash(DigestUtils.md5DigestAsHex(prompt.getBytes(StandardCharsets.UTF_8)));
                AiProperties.FeatureConfig featureConfig = providerRouter.getFeatureConfig(FEATURE_KEY);
                AiChatRequest request = AiChatRequest.builder()
                        .model(model)
                        .userPrompt(prompt)
                        .temperature(0.1)
                        .maxTokens(4096)
                        .timeout(featureConfig.getTimeout())
                        .retry(featureConfig.getRetry())
                        .build();

                // 所有校验与解析完成后、发起 LLM 调用前才扣减配额
                consumeQuota(userId, guest);
                GrammarCheckResponse result;
                try {
                    result = doCheck(provider, request);
                } catch (Exception ex) {
                    // LLM 调用失败退还本次配额
                    aiDailyQuotaService.refund(userId, FEATURE_KEY);
                    throw ex;
                }

                callLog.setSuccess(true);
                callLog.setLatencyMs(System.currentTimeMillis() - start);
                auditLogService.record(userId, "ai_grammar_check", resumeId,
                        "issues=" + result.getIssues().size());
                return result;
            } catch (BusinessException ex) {
                callLog.setSuccess(false);
                callLog.setErrorMsg(ex.getMessage());
                throw ex;
            } catch (Exception ex) {
                callLog.setSuccess(false);
                callLog.setErrorMsg(ex.getMessage());
                throw new BusinessException(ResultCode.AI_MODEL_CALL_FAILED, "语法检查失败，请稍后重试。");
            } finally {
                callLog.setLatencyMs(System.currentTimeMillis() - start);
                AiCallLogDefaults.fillRequiredColumns(callLog, provider.getProviderName());
                try {
                    aiCallLogMapper.insert(callLog);
                } catch (Exception logEx) {
                    log.warn("Insert grammar AiCallLog failed: {}", logEx.getMessage());
                }
            }
        } finally {
            releaseInFlight(userId, counter);
        }
    }

    private GrammarCheckResponse doCheck(LlmProvider provider, AiChatRequest request) {
        GrammarCheckResponse result = provider.chatStructured(request, GrammarCheckResponse.class);
        if (result == null) {
            throw new BusinessException(ResultCode.AI_RESPONSE_PARSE_FAILED, "语法检查结果为空。");
        }
        result.setStatus("success");
        result.setModel(request.getModel());
        result.setCheckedAt(LocalDateTime.now());
        result.setIssues(result.getIssues() == null ? List.of() : result.getIssues());
        return result;
    }

    /**
     * 释放并发计数，并在计数归零时清理内存条目，避免 inFlight Map 无限增长。
     */
    private void releaseInFlight(String userId, AtomicInteger counter) {
        if (counter.decrementAndGet() <= 0) {
            inFlight.remove(userId, counter);
        }
    }

    /**
     * 按日配额扣减：游客与登录用户使用不同上限，与 AI 写作复用同一 quota 类型。
     */
    private void consumeQuota(String userId, boolean guest) {
        int dailyLimit = guest
                ? aiProperties.getDailyQuota().getGuest()
                : aiProperties.getDailyQuota().getUser();
        aiDailyQuotaService.consume(userId, FEATURE_KEY, dailyLimit);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }

    private boolean hasProviderKey(String provider) {
        return switch (provider) {
            case "openai" -> aiProperties.getOpenai() != null
                    && StringUtils.isNotBlank(aiProperties.getOpenai().getApiKey());
            case "qwen" -> aiProperties.getQwen() != null
                    && StringUtils.isNotBlank(aiProperties.getQwen().getApiKey());
            case "ernie" -> aiProperties.getErnie() != null
                    && StringUtils.isNotBlank(aiProperties.getErnie().getApiKey());
            default -> false;
        };
    }
}
