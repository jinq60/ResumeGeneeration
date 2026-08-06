package com.resume.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.ai.config.AiPromptTemplates;
import com.resume.ai.config.AiProperties;
import com.resume.ai.dto.AiChatRequest;
import com.resume.ai.dto.AiChatResponse;
import com.resume.ai.dto.GrammarCheckResponse;
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

/**
 * AI 简历语法检查：返回可定位到 Section 字段的修改建议。
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
    private final ObjectMapper objectMapper;

    public GrammarCheckResponse check(String userId, String resumeId) {
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
                return fallback;
            }
            throw ex;
        }

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
            callLog.setRequestHash(String.valueOf(prompt.hashCode()));
            AiChatRequest request = AiChatRequest.builder()
                    .model(model)
                    .userPrompt(prompt)
                    .temperature(0.1)
                    .maxTokens(4096)
                    .build();

            GrammarCheckResponse result = provider.chatStructured(request, GrammarCheckResponse.class);
            if (result == null) {
                throw new BusinessException(ResultCode.AI_RESPONSE_PARSE_FAILED, "语法检查结果为空。");
            }
            result.setStatus("success");
            result.setModel(model);
            result.setCheckedAt(LocalDateTime.now());
            result.setIssues(result.getIssues() == null ? List.of() : result.getIssues());
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
            try {
                aiCallLogMapper.insert(callLog);
            } catch (Exception logEx) {
                log.warn("Insert grammar AiCallLog failed: {}", logEx.getMessage());
            }
        }
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
