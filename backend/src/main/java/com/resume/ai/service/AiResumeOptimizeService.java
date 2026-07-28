package com.resume.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.ai.config.AiPromptTemplates;
import com.resume.ai.dto.AiChatRequest;
import com.resume.ai.dto.AiChatResponse;
import com.resume.ai.dto.ResumeOptimizeResponse.SectionOptimization;
import com.resume.ai.entity.AiCallLog;
import com.resume.ai.entity.ResumeOptimizeTask;
import com.resume.ai.mapper.AiCallLogMapper;
import com.resume.ai.mapper.ResumeOptimizeTaskMapper;
import com.resume.ai.provider.LlmProvider;
import com.resume.ai.provider.ProviderRouter;
import com.resume.common.constant.BizConstant;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.resume.dto.SectionDTO;
import com.resume.resume.entity.Resume;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI JD 匹配优化服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiResumeOptimizeService {

    private static final String FEATURE_KEY = "resume-optimize";

    private final ResumeOptimizeTaskMapper optimizeTaskMapper;
    private final AiCallLogMapper aiCallLogMapper;
    private final ProviderRouter providerRouter;
    private final AiPromptTemplates promptTemplates;
    private final ObjectMapper objectMapper;

    @Async("aiTaskExecutor")
    public void executeOptimize(String taskId, Resume resume, String jobDescription) {
        ResumeOptimizeTask task = optimizeTaskMapper.selectById(taskId);
        if (task == null) {
            log.error("Optimize task not found: {}", taskId);
            return;
        }

        task.setStatus(BizConstant.TASK_STATUS_PROCESSING);
        task.setUpdatedAt(LocalDateTime.now());
        optimizeTaskMapper.updateById(task);

        long start = System.currentTimeMillis();
        AiCallLog callLog = new AiCallLog();
        callLog.setUserId(resume.getUserId());
        callLog.setFeatureKey(FEATURE_KEY);
        callLog.setCreatedAt(LocalDateTime.now());

        try {
            LlmProvider provider = providerRouter.resolve(FEATURE_KEY);
            String model = providerRouter.resolveModel(FEATURE_KEY);
            log.info("AI optimize chat -> provider={}, model={}, taskId={}",
                    provider.getProviderName(), model, taskId);

            String resumeContent = toJson(resume.getSections());
            String prompt = promptTemplates.render(FEATURE_KEY, Map.of(
                    "resumeContent", resumeContent,
                    "jobDescription", jobDescription
            ));

            AiChatRequest request = AiChatRequest.builder()
                    .model(model)
                    .userPrompt(prompt)
                    .temperature(0.3)
                    .maxTokens(4096)
                    .build();

            AiChatResponse response = provider.chat(request);

            callLog.setProviderName(provider.getProviderName());
            callLog.setModelName(model);
            callLog.setRequestHash(String.valueOf(prompt.hashCode()));
            callLog.setPromptTokens(response.getPromptTokens());
            callLog.setCompletionTokens(response.getCompletionTokens());
            callLog.setTotalTokens(response.getTotalTokens());
            callLog.setLatencyMs(response.getLatencyMs());
            callLog.setSuccess(response.isSuccess());

            if (response.isSuccess()) {
                parseAndFillResult(task, response.getContent(), model);
                task.setStatus(BizConstant.TASK_STATUS_SUCCESS);
            } else {
                task.setStatus(BizConstant.TASK_STATUS_FAILED);
                task.setErrorMsg(response.getErrorMsg());
                callLog.setErrorMsg(response.getErrorMsg());
            }
        } catch (Exception e) {
            log.error("AI optimize failed for taskId={}", taskId, e);
            task.setStatus(BizConstant.TASK_STATUS_FAILED);
            task.setErrorMsg(e.getMessage());
            callLog.setSuccess(false);
            callLog.setErrorMsg(e.getMessage());
            callLog.setLatencyMs(System.currentTimeMillis() - start);
        } finally {
            // 兜底：若状态未被显式设置，标记为 failed，避免任务永久卡在 processing
            if (BizConstant.TASK_STATUS_PROCESSING.equals(task.getStatus())) {
                log.error("Optimize task ended in processing state, marking failed: taskId={}", taskId);
                task.setStatus(BizConstant.TASK_STATUS_FAILED);
                task.setErrorMsg("任务执行异常结束");
            }
            task.setUpdatedAt(LocalDateTime.now());
            if (BizConstant.TASK_STATUS_SUCCESS.equals(task.getStatus())) {
                task.setCompletedAt(LocalDateTime.now());
            }
            optimizeTaskMapper.updateById(task);
            try {
                aiCallLogMapper.insert(callLog);
            } catch (Exception logEx) {
                log.warn("Insert AiCallLog failed for taskId={}: {}", taskId, logEx.getMessage());
            }
        }
    }

    private void parseAndFillResult(ResumeOptimizeTask task, String content, String model) {
        try {
            String cleaned = content.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("```\\w*\\n?", "").replaceAll("```\\n?", "").trim();
            }
            JsonNode root = objectMapper.readTree(cleaned);

            task.setMatchScore(root.get("matchScore").asInt());
            task.setDimensionScores(objectMapper.convertValue(root.get("dimensionScores"), Map.class));
            task.setOptimizations(objectMapper.convertValue(root.get("optimizations"),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, SectionOptimization.class)));
            task.setMissingSkills(objectMapper.convertValue(root.get("missingSkills"), List.class));
            task.setRecommendations(objectMapper.convertValue(root.get("recommendations"), List.class));
            task.setModelName(model);
        } catch (Exception e) {
            log.error("Failed to parse optimize result", e);
            throw new BusinessException(ResultCode.AI_RESPONSE_PARSE_FAILED, "AI 优化结果解析失败。");
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
