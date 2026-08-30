package com.resume.ai.service;

import com.resume.ai.config.AiPromptTemplates;
import com.resume.ai.config.AiProperties;
import com.resume.ai.dto.AiChatRequest;
import com.resume.ai.dto.AiChatResponse;
import com.resume.ai.entity.AiCallLog;
import com.resume.ai.mapper.AiCallLogMapper;
import com.resume.ai.provider.LlmProvider;
import com.resume.ai.provider.ProviderRouter;
import com.resume.ai.util.AiCallLogDefaults;
import com.resume.avatar.entity.AvatarTask;
import com.resume.avatar.mapper.AvatarTaskMapper;
import com.resume.common.constant.BizConstant;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.common.service.MinioStorageService;
import com.resume.common.util.ImageMagicUtil;
import com.resume.resume.service.ResumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * AI 头像一寸照优化服务 —— 异步调用多模态 LLM 分析自拍照。
 * <p>
 * 流程：读取任务 → 下载原图 → 转 base64 → 多模态分析 → 落库结果。
 * 当前阶段多模态 LLM 返回「优化描述」，真实的一寸照图像生成需接入专门的图像生成模型
 * （如通义万相 / DALL-E），生成后回写 {@code resultImageUrl}。
 * 未配置多模态供应商时降级为复用原图（保持 P0 占位行为）。
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
    @Lazy
    private final ResumeService resumeService;
    private final com.resume.notification.service.NotificationService notificationService;

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
            AiProperties.FeatureConfig featureConfig = providerRouter.getFeatureConfig(FEATURE_KEY);

            String dataUrl = toDataUrl(task.getSourceImageUrl());
            String prompt = promptTemplates.render(FEATURE_KEY, Map.of(
                    "backgroundType", StringUtils.defaultString(task.getBackgroundType()),
                    "style", StringUtils.defaultString(task.getStyle())
            ));

            AiChatRequest request = AiChatRequest.builder()
                    .model(model)
                    .messages(List.of(
                            AiChatRequest.Message.builder()
                                    .role("user")
                                    .contents(List.of(
                                            AiChatRequest.ContentPart.builder().type("text").text(prompt).build(),
                                            AiChatRequest.ContentPart.builder().type("image_url").imageUrl(dataUrl).build()
                                    ))
                                    .build()
                    ))
                    .temperature(0.3)
                    .maxTokens(1024)
                    .timeout(featureConfig.getTimeout())
                    .retry(featureConfig.getRetry())
                    .build();

            AiChatResponse response = provider.chat(request);

            callLog.setProviderName(provider.getProviderName());
            callLog.setModelName(model);
            callLog.setRequestHash(DigestUtils.md5DigestAsHex(prompt.getBytes(StandardCharsets.UTF_8)));
            callLog.setPromptTokens(response.getPromptTokens());
            callLog.setCompletionTokens(response.getCompletionTokens());
            callLog.setTotalTokens(response.getTotalTokens());
            callLog.setLatencyMs(response.getLatencyMs());
            callLog.setSuccess(response.isSuccess());

            if (response.isSuccess()) {
                mergeAiDescription(task, response.getContent());
                // 真实图像生成待接入专门模型，当前复用原图作为结果
                task.setResultImageUrl(task.getSourceImageUrl());
                task.setStatus(BizConstant.TASK_STATUS_SUCCESS);
                task.setCompletedAt(LocalDateTime.now());
            } else {
                task.setStatus(BizConstant.TASK_STATUS_FAILED);
                task.setErrorMsg(response.getErrorMsg());
                callLog.setErrorMsg(response.getErrorMsg());
            }
        } catch (BusinessException e) {
            // 多模态供应商未配置：降级为复用原图，保持 P0 占位行为
            log.warn("AI avatar optimize provider not configured, fallback to source image: {}", e.getMessage());
            task.setResultImageUrl(task.getSourceImageUrl());
            task.setStatus(BizConstant.TASK_STATUS_SUCCESS);
            task.setCompletedAt(LocalDateTime.now());
            callLog.setSuccess(false);
            callLog.setErrorMsg("fallback: " + e.getMessage());
            callLog.setLatencyMs(System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("AI avatar optimize failed for taskId={}", taskId, e);
            task.setStatus(BizConstant.TASK_STATUS_FAILED);
            task.setErrorMsg(e.getMessage());
            callLog.setSuccess(false);
            callLog.setErrorMsg(e.getMessage());
            callLog.setLatencyMs(System.currentTimeMillis() - start);
        } finally {
            task.setUpdatedAt(LocalDateTime.now());
            avatarTaskMapper.updateById(task);

            // 成功后回填一寸照地址到简历 profile.avatarUrl（失败不影响任务结果）
            if (BizConstant.TASK_STATUS_SUCCESS.equals(task.getStatus())
                    && StringUtils.isNotBlank(task.getResumeId())
                    && StringUtils.isNotBlank(task.getResultImageUrl())) {
                try {
                    resumeService.fillAvatarUrl(task.getUserId(), task.getResumeId(), task.getResultImageUrl());
                } catch (Exception e) {
                    log.warn("fillAvatarUrl failed for userId={}, resumeId={}: {}",
                            task.getUserId(), task.getResumeId(), e.getMessage());
                }
            }

            if (BizConstant.TASK_STATUS_SUCCESS.equals(task.getStatus()) && task.getUserId() != null) {
                notificationService.notify(task.getUserId(), "avatar", "头像优化完成",
                        "你的头像一寸照优化已完成，可前往下载中心查看。");
            }

            try {
                // 失败调用也要落审计表：NOT NULL 列兜底
                AiCallLogDefaults.fillRequiredColumns(callLog, null);
                aiCallLogMapper.insert(callLog);
            } catch (Exception logEx) {
                log.warn("Insert AiCallLog failed for taskId={}: {}", taskId, logEx.getMessage());
            }
        }
    }

    /**
     * 下载原图并转为 data URL，供多模态模型读取。
     */
    private String toDataUrl(String sourceImageUrl) {
        String objectName = extractObjectName(sourceImageUrl);
        if (StringUtils.isBlank(objectName)) {
            throw new BusinessException(ResultCode.AVATAR_SOURCE_NOT_FOUND, "头像原图不存在。");
        }
        byte[] image = minioStorageService.download(minioStorageService.getBucketAvatars(), objectName);
        String format = ImageMagicUtil.detectFormat(image);
        String contentType = ImageMagicUtil.toContentType(format);
        if (contentType == null) {
            contentType = "image/jpeg";
        }
        return "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(image);
    }

    private String extractObjectName(String sourceImageUrl) {
        if (StringUtils.isBlank(sourceImageUrl)) {
            return null;
        }
        String trimmed = sourceImageUrl.trim();
        // 兼容多种形式：/uploads/avatars/...、http(s)://host/.../uploads/avatars/...、带查询参数的预签名 URL
        String withoutQuery = trimmed.split("\\?")[0];
        // 去掉可能的 http(s)://host 前缀
        int uploadsIdx = withoutQuery.indexOf("/uploads/avatars/");
        if (uploadsIdx >= 0) {
            return withoutQuery.substring(uploadsIdx + "/uploads/avatars/".length());
        }
        // 兼容直接存储为 bucket 前缀（如 resume-avatars/userId/...）
        if (withoutQuery.startsWith(minioStorageService.getBucketAvatars() + "/")) {
            return withoutQuery.substring(minioStorageService.getBucketAvatars().length() + 1);
        }
        return null;
    }

    /**
     * 将 AI 返回的优化描述合并进任务 options（Map），供前端展示。
     * AvatarTask.options 已改为 Map<String, Object> + JacksonTypeHandler，
     * 直接读写 Map 即可，MyBatis-Plus 会负责 JSON 序列化。
     */
    private void mergeAiDescription(AvatarTask task, String description) {
        Map<String, Object> options = task.getOptions() != null
                ? task.getOptions()
                : new java.util.HashMap<>();
        options.put("aiDescription", description == null ? "" : description);
        task.setOptions(options);
    }
}
