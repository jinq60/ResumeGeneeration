package com.resume.avatar.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.avatar.dto.AvatarTaskResponse;
import com.resume.avatar.dto.AvatarUploadResponse;
import com.resume.avatar.dto.OptimizeAvatarRequest;
import com.resume.avatar.entity.AvatarTask;
import com.resume.avatar.mapper.AvatarTaskMapper;
import com.resume.common.constant.BizConstant;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.common.service.MinioStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 头像上传与优化任务服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AvatarService {

    private final AvatarTaskMapper avatarTaskMapper;
    private final MinioStorageService minioStorageService;
    private final ObjectMapper objectMapper;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final String[] ALLOWED_CONTENT_TYPES = {"image/jpeg", "image/png", "image/webp"};
    private static final String[] ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".webp"};

    /**
     * 上传自拍照。
     */
    @Transactional(rollbackFor = Exception.class)
    public AvatarUploadResponse uploadAvatar(String userId, MultipartFile file, String resumeId) {
        validateAvatarFile(file);

        String originalFilename = StringUtils.defaultString(file.getOriginalFilename(), "avatar.png");
        String ext = getExtension(originalFilename);
        String avatarId = "avatar_" + System.currentTimeMillis();
        String objectName = userId + "/avatars/" + avatarId + "_source" + ext;

        try {
            minioStorageService.upload(minioStorageService.getBucketAvatars(), objectName,
                    file.getInputStream(), file.getSize(), file.getContentType());
        } catch (IOException e) {
            throw new BusinessException(ResultCode.AVATAR_OPTIMIZE_FAILED, "头像上传失败。");
        }

        // P0：原图 URL 使用相对路径，便于开发环境演示。
        String sourceUrl = "/uploads/avatars/" + objectName;

        AvatarTask task = new AvatarTask();
        task.setId(avatarId);
        task.setUserId(userId);
        task.setResumeId(resumeId);
        task.setSourceImageUrl(sourceUrl);
        task.setBackgroundType(BizConstant.AVATAR_BACKGROUND_WHITE);
        task.setStyle(BizConstant.AVATAR_STYLE_FORMAL);
        task.setOptions(toJson(defaultOptions()));
        task.setStatus(BizConstant.TASK_STATUS_SUCCESS);
        task.setResultImageUrl(sourceUrl);
        task.setCompletedAt(LocalDateTime.now());
        task.setDeleted(BizConstant.NOT_DELETED);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        avatarTaskMapper.insert(task);

        AvatarUploadResponse response = new AvatarUploadResponse();
        response.setId(avatarId);
        response.setSourceImageUrl(sourceUrl);
        response.setFileName(originalFilename);
        return response;
    }

    /**
     * 创建一寸照优化任务。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> optimizeAvatar(String userId, OptimizeAvatarRequest request) {
        // 校验背景类型：P0 仅支持 white/blue/red
        if (!Arrays.asList(BizConstant.AVATAR_BACKGROUND_TYPES_P0).contains(request.getBackgroundType())) {
            throw new BusinessException(ResultCode.AVATAR_BACKGROUND_TYPE_INVALID, "背景类型不正确，P0 仅支持 white/blue/red。");
        }
        if (!Arrays.asList(BizConstant.AVATAR_STYLES).contains(request.getStyle())) {
            throw new BusinessException(ResultCode.AVATAR_STYLE_INVALID, "照片风格不正确。");
        }

        // P0 占位：直接复用原图作为结果。
        String taskId = "avatar_task_" + System.currentTimeMillis();
        AvatarTask task = new AvatarTask();
        task.setId(taskId);
        task.setUserId(userId);
        task.setResumeId(request.getResumeId());
        task.setSourceImageUrl(request.getSourceImageUrl());
        task.setResultImageUrl(request.getSourceImageUrl());
        task.setBackgroundType(request.getBackgroundType());
        task.setStyle(request.getStyle());
        task.setOptions(toJson(buildOptions(request)));
        task.setStatus(BizConstant.TASK_STATUS_PENDING);
        task.setDeleted(BizConstant.NOT_DELETED);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        avatarTaskMapper.insert(task);

        // P0 占位：同步完成优化，状态机从 pending -> processing -> success。
        transitionStatus(task, BizConstant.TASK_STATUS_PROCESSING);
        task.setUpdatedAt(LocalDateTime.now());
        avatarTaskMapper.updateById(task);

        transitionStatus(task, BizConstant.TASK_STATUS_SUCCESS);
        task.setCompletedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        avatarTaskMapper.updateById(task);

        Map<String, Object> result = new HashMap<>();
        result.put("taskId", taskId);
        result.put("status", BizConstant.TASK_STATUS_SUCCESS);
        return result;
    }

    /**
     * 查询头像优化任务。
     */
    public AvatarTaskResponse getTask(String userId, String taskId) {
        AvatarTask task = avatarTaskMapper.selectById(taskId);
        if (task == null || BizConstant.DELETED.equals(task.getDeleted())) {
            throw new BusinessException(ResultCode.AVATAR_TASK_NOT_FOUND, "头像优化任务不存在。");
        }
        if (!userId.equals(task.getUserId())) {
            throw new BusinessException(ResultCode.ACCESS_DENIED, "无权访问该资源。");
        }
        return toResponse(task);
    }

    /**
     * 删除头像及优化记录。
     * <p>
     * 校验所有权后删除 MinIO 原图，并逻辑删除所有关联任务记录。
     * </p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteAvatar(String userId, String avatarId) {
        AvatarTask task = avatarTaskMapper.selectById(avatarId);
        if (task == null || BizConstant.DELETED.equals(task.getDeleted())) {
            throw new BusinessException(ResultCode.AVATAR_TASK_NOT_FOUND, "头像记录不存在。");
        }
        if (!userId.equals(task.getUserId())) {
            throw new BusinessException(ResultCode.ACCESS_DENIED, "无权访问该资源。");
        }

        String sourceImageUrl = task.getSourceImageUrl();
        String sourceObjectName = extractObjectName(sourceImageUrl);
        if (StringUtils.isNotBlank(sourceObjectName)) {
            minioStorageService.remove(minioStorageService.getBucketAvatars(), sourceObjectName);
        }
        String resultImageUrl = task.getResultImageUrl();
        String resultObjectName = extractObjectName(resultImageUrl);
        if (StringUtils.isNotBlank(resultObjectName) && !resultObjectName.equals(sourceObjectName)) {
            minioStorageService.remove(minioStorageService.getBucketAvatars(), resultObjectName);
        }

        AvatarTask deleted = new AvatarTask();
        deleted.setDeleted(BizConstant.DELETED);
        LambdaQueryWrapper<AvatarTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AvatarTask::getUserId, userId)
               .eq(AvatarTask::getSourceImageUrl, sourceImageUrl);
        avatarTaskMapper.update(deleted, wrapper);
    }

    private String extractObjectName(String sourceImageUrl) {
        if (StringUtils.isBlank(sourceImageUrl)) {
            return null;
        }
        String prefix = "/uploads/avatars/";
        if (sourceImageUrl.startsWith(prefix)) {
            return sourceImageUrl.substring(prefix.length());
        }
        return null;
    }

    private void transitionStatus(AvatarTask task, String newStatus) {
        String current = task.getStatus();
        if (BizConstant.TASK_STATUS_SUCCESS.equals(current) || BizConstant.TASK_STATUS_FAILED.equals(current)) {
            throw new BusinessException(ResultCode.AVATAR_OPTIMIZE_FAILED, "任务已结束，不可变更状态。");
        }
        if (BizConstant.TASK_STATUS_PENDING.equals(current)
                && !BizConstant.TASK_STATUS_PROCESSING.equals(newStatus)) {
            throw new BusinessException(ResultCode.AVATAR_OPTIMIZE_FAILED,
                    "仅允许从 pending 转为 processing。");
        }
        if (BizConstant.TASK_STATUS_PROCESSING.equals(current)
                && !BizConstant.TASK_STATUS_SUCCESS.equals(newStatus)
                && !BizConstant.TASK_STATUS_FAILED.equals(newStatus)) {
            throw new BusinessException(ResultCode.AVATAR_OPTIMIZE_FAILED,
                    "仅允许从 processing 转为 success 或 failed。");
        }
        task.setStatus(newStatus);
    }

    private void validateAvatarFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.AVATAR_FILE_EMPTY, "请上传头像图片。");
        }
        if (!Arrays.asList(ALLOWED_CONTENT_TYPES).contains(file.getContentType())) {
            throw new BusinessException(ResultCode.AVATAR_FORMAT_UNSUPPORTED, "请上传 JPG、PNG 或 WEBP 格式图片。");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ResultCode.AVATAR_FILE_TOO_LARGE, "图片大小不能超过 10MB。");
        }
    }

    private String getExtension(String filename) {
        String lower = filename.toLowerCase();
        for (String ext : ALLOWED_EXTENSIONS) {
            if (lower.endsWith(ext)) {
                return ext;
            }
        }
        return ".png";
    }

    private Map<String, Object> defaultOptions() {
        Map<String, Object> options = new HashMap<>();
        options.put("keepIdentity", true);
        options.put("enhanceQuality", false);
        options.put("removeBackground", false);
        options.put("brightenSkin", false);
        return options;
    }

    private Map<String, Object> buildOptions(OptimizeAvatarRequest request) {
        Map<String, Object> options = new HashMap<>();
        options.put("keepIdentity", request.getKeepIdentity() != null ? request.getKeepIdentity() : true);
        options.put("enhanceQuality", request.getEnhanceQuality() != null ? request.getEnhanceQuality() : false);
        options.put("removeBackground", request.getRemoveBackground() != null ? request.getRemoveBackground() : false);
        options.put("brightenSkin", request.getBrightenSkin() != null ? request.getBrightenSkin() : false);
        return options;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "参数序列化失败。");
        }
    }

    private AvatarTaskResponse toResponse(AvatarTask task) {
        AvatarTaskResponse response = new AvatarTaskResponse();
        response.setTaskId(task.getId());
        response.setStatus(task.getStatus());
        response.setSourceImageUrl(task.getSourceImageUrl());
        response.setResultImageUrl(task.getResultImageUrl());
        response.setErrorMsg(task.getErrorMsg());
        response.setCreatedAt(task.getCreatedAt());
        response.setCompletedAt(task.getCompletedAt());
        return response;
    }
}
