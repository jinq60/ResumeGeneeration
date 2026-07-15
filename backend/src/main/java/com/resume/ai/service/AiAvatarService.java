package com.resume.ai.service;

import com.resume.ai.config.AiPromptTemplates;
import com.resume.ai.dto.AiChatRequest;
import com.resume.ai.dto.AiChatResponse;
import com.resume.ai.entity.AiCallLog;
import com.resume.ai.mapper.AiCallLogMapper;
import com.resume.ai.provider.LlmProvider;
import com.resume.ai.provider.ProviderRouter;
import com.resume.avatar.entity.AvatarTask;
import com.resume.avatar.mapper.AvatarTaskMapper;
import com.resume.common.constant.BizConstant;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.common.service.MinioStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * AI 头像一寸照优化服务 —— 异步调用多模态 LLM。
 * <p>
 * P0 占位：同步复制原图。P1 异步调用真实 AI 模型。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAvatarService {

    private static final String FEATURE_KEY = "avatar-optimize";

    private final AvatarTaskMapper avatarTaskMapper;
    private final AiCallLogMapper aiCallLogMapper;
    private final ProviderRouter providerRouter;
    private final AiPromptTemplates promptTemplates;
    private final MinioStorageService minioStorageService;

    /**
     * P1：异步执行真实 AI 头像优化。
     * <p>
     * 当前 P0 阶段保持同步占位逻辑，此方法留作 P1 接入。
     * </p>
     */
    @Async("aiTaskExecutor")
    public void executeOptimize(String taskId) {
        AvatarTask task = avatarTaskMapper.selectById(taskId);
        if (task == null) {
            log.error("Avatar task not found: {}", taskId);
            return;
        }

        task.setStatus(BizConstant.TASK_STATUS_PROCESSING);
        task.setUpdatedAt(LocalDateTime.now());
        avatarTaskMapper.updateById(task);

        long start = System.currentTimeMillis();
        AiCallLog callLog = new AiCallLog();
        callLog.setUserId(task.getUserId());
        callLog.setFeatureKey(FEATURE_KEY);
        callLog.setCreatedAt(LocalDateTime.now());

        try {
            LlmProvider provider = providerRouter.resolve(FEATURE_KEY);
            String model = providerRouter.resolveModel(FEATURE_KEY);

            String prompt = promptTemplates.render(FEATURE_KEY, Map.of(
                    "backgroundType", task.getBackgroundType(),
                    "style", task.getStyle()
            ));

            AiChatRequest request = AiChatRequest.builder()
                    .model(model)
                    .userPrompt(prompt)
                    .temperature(0.2)
                    .maxTokens(1024)
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
                // P1: 解析 AI 返回的图像 URL 或 base64，上传到 MinIO
                task.setStatus(BizConstant.TASK_STATUS_SUCCESS);
            } else {
                task.setStatus(BizConstant.TASK_STATUS_FAILED);
                task.setErrorMsg(response.getErrorMsg());
                callLog.setErrorMsg(response.getErrorMsg());
            }
        } catch (Exception e) {
            log.error("AI avatar optimize failed for taskId={}", taskId, e);
            task.setStatus(BizConstant.TASK_STATUS_FAILED);
            task.setErrorMsg(e.getMessage());
            callLog.setSuccess(false);
            callLog.setErrorMsg(e.getMessage());
            callLog.setLatencyMs(System.currentTimeMillis() - start);
        }

        if (BizConstant.TASK_STATUS_SUCCESS.equals(task.getStatus())) {
            task.setCompletedAt(LocalDateTime.now());
        }
        task.setUpdatedAt(LocalDateTime.now());
        avatarTaskMapper.updateById(task);
        aiCallLogMapper.insert(callLog);
    }
}
