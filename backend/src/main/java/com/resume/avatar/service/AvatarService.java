package com.resume.avatar.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.resume.ai.service.AiAvatarService;
import com.resume.avatar.dto.AvatarTaskResponse;
import com.resume.avatar.dto.AvatarUploadResponse;
import com.resume.avatar.dto.OptimizeAvatarRequest;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
    private final ResumeService resumeService;
    @Lazy
    private final AiAvatarService aiAvatarService;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final String[] ALLOWED_CONTENT_TYPES = {"image/jpeg", "image/png", "image/webp"};

    /**
     * 上传自拍照。
     */
    @Transactional(rollbackFor = Exception.class)
    public AvatarUploadResponse uploadAvatar(String userId, MultipartFile file, String resumeId) {
        String imageFormat = validateAvatarFile(file);

        // 校验简历归属：非空时必须属于当前用户，防止把他人简历与本次头像关联
        if (StringUtils.isNotBlank(resumeId)) {
            resumeService.getResumeEntity(userId, resumeId);
        }

        String originalFilename = StringUtils.defaultString(file.getOriginalFilename(), "avatar.png");
        // 扩展名与 Content-Type 均由魔数识别结果决定，避免伪造文件名/Content-Type 造成后缀与内容不一致
        String ext = "." + imageFormat;
        String contentType = "image/" + ("jpg".equals(imageFormat) ? "jpeg" : imageFormat);
        // 先预生成任务 ID，用于拼对象名与在插入前完成 MinIO 上传（source_image_url 为 NOT NULL）
        String taskId = com.baomidou.mybatisplus.core.toolkit.IdWorker.getIdStr();
        String objectName = userId + "/avatars/" + taskId + "_source" + ext;
        String sourceUrl = "/uploads/avatars/" + objectName;

        try {
            minioStorageService.upload(minioStorageService.getBucketAvatars(), objectName,
                    file.getInputStream(), file.getSize(), contentType);
            // 注册事务回滚时清理孤儿文件：若后续 DB 插入失败导致事务回滚，MinIO 已上传对象会被清理
            minioStorageService.removeOnRollback(minioStorageService.getBucketAvatars(), objectName);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.AVATAR_OPTIMIZE_FAILED, "头像上传失败。");
        } catch (BusinessException e) {
            // MinIO 上传阶段已抛 BusinessException（内部已包装），无需重复清理（removeOnRollback 已注册）
            throw e;
        } catch (Exception e) {
            // 其他未知异常：尝试即时清理避免部分写入
            minioStorageService.remove(minioStorageService.getBucketAvatars(), objectName);
            throw new BusinessException(ResultCode.AVATAR_OPTIMIZE_FAILED, "头像上传失败。");
        }

        AvatarTask task = new AvatarTask();
        task.setId(taskId);
        task.setUserId(userId);
        task.setResumeId(resumeId);
        task.setSourceImageUrl(sourceUrl);
        task.setResultImageUrl(sourceUrl);
        task.setBackgroundType(BizConstant.AVATAR_BACKGROUND_WHITE);
        task.setStyle(BizConstant.AVATAR_STYLE_FORMAL);
        task.setOptions(defaultOptions());
        task.setStatus(BizConstant.TASK_STATUS_SUCCESS);
        task.setCompletedAt(LocalDateTime.now());
        task.setDeleted(BizConstant.NOT_DELETED);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        try {
            avatarTaskMapper.insert(task);
        } catch (Exception e) {
            // 插入失败（事务将回滚）时 best-effort 删除已上传对象，避免 MinIO 孤儿文件；
            // 清理失败仅 warn，不掩盖原始异常
            try {
                minioStorageService.remove(minioStorageService.getBucketAvatars(), objectName);
            } catch (Exception cleanupEx) {
                log.warn("Failed to cleanup uploaded avatar object after insert failure: {}", objectName, cleanupEx);
            }
            throw e;
        }

        AvatarUploadResponse response = new AvatarUploadResponse();
        response.setId(task.getId());
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
        if (!BizConstant.AVATAR_BACKGROUND_TYPES_P0.contains(request.getBackgroundType())) {
            throw new BusinessException(ResultCode.AVATAR_BACKGROUND_TYPE_INVALID, "背景类型不正确，P0 仅支持 white/blue/red。");
        }
        if (!BizConstant.AVATAR_STYLES.contains(request.getStyle())) {
            throw new BusinessException(ResultCode.AVATAR_STYLE_INVALID, "照片风格不正确。");
        }

        // 校验原图归属：只允许使用当前用户自己上传的头像，防止越权引用他人对象
        assertOwnAvatarUrl(userId, request.getSourceImageUrl());

        // 校验简历归属：非空时必须属于当前用户
        if (StringUtils.isNotBlank(request.getResumeId())) {
            resumeService.getResumeEntity(userId, request.getResumeId());
        }

        // 创建待处理任务，异步交由 AI 多模态分析；前端通过 GET /avatars/tasks/{taskId} 轮询结果
        AvatarTask task = new AvatarTask();
        task.setUserId(userId);
        task.setResumeId(request.getResumeId());
        task.setSourceImageUrl(request.getSourceImageUrl());
        task.setResultImageUrl(request.getSourceImageUrl());
        task.setBackgroundType(request.getBackgroundType());
        task.setStyle(request.getStyle());
        task.setOptions(buildOptions(request));
        task.setStatus(BizConstant.TASK_STATUS_PENDING);
        task.setDeleted(BizConstant.NOT_DELETED);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        avatarTaskMapper.insert(task);

        // 异步任务必须在事务提交后触发：事务内立即启动 @Async 时，异步线程在 REPEATABLE READ
        // 下读不到未提交的任务行，会误判 "task not found" 导致任务永久 pending
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    submitOptimize(task.getId());
                }
            });
        } else {
            submitOptimize(task.getId());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("taskId", task.getId());
        result.put("status", BizConstant.TASK_STATUS_PENDING);
        return result;
    }

    /**
     * 提交一寸照优化到 AI 线程池；队列满时将任务标记为失败，避免永久卡在 pending。
     */
    private void submitOptimize(String taskId) {
        try {
            aiAvatarService.executeOptimize(taskId);
        } catch (org.springframework.core.task.TaskRejectedException e) {
            log.warn("Avatar optimize task rejected (thread pool exhausted): taskId={}", taskId);
            AvatarTask failed = new AvatarTask();
            failed.setId(taskId);
            failed.setStatus(BizConstant.TASK_STATUS_FAILED);
            failed.setErrorMsg("AI 服务繁忙，请稍后再试。");
            failed.setUpdatedAt(LocalDateTime.now());
            avatarTaskMapper.updateById(failed);
        }
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
     * 校验所有权后逻辑删除所有关联任务记录；MinIO 原图/结果图延迟到事务提交后
     * 移除，若事务回滚则文件保留，避免出现"记录在、文件没了"的不一致。
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

        // 逻辑删除字段必须用 UpdateWrapper.set 显式写入：实体方式会被 MP 从 SET 子句排除导致静默失效
        LambdaUpdateWrapper<AvatarTask> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AvatarTask::getUserId, userId)
               .eq(AvatarTask::getSourceImageUrl, task.getSourceImageUrl())
               .set(AvatarTask::getDeleted, BizConstant.DELETED);
        avatarTaskMapper.update(null, wrapper);

        // 物理文件清理需防误删：仅当无其他活跃任务仍引用该对象时才删 MinIO 文件
        // deleteAvatar 按 sourceImageUrl 全量删除，清理后 hasOtherActiveReference 恒为 0，但保留检查以防御未来逻辑变更
        List<String> pendingRemoval = new ArrayList<>();
        String sourceObjectName = extractObjectName(task.getSourceImageUrl());
        if (StringUtils.isNotBlank(sourceObjectName) && isOwnedObject(userId, sourceObjectName)
                && !hasOtherActiveReference(userId, task.getSourceImageUrl())) {
            pendingRemoval.add(sourceObjectName);
        }
        String resultObjectName = extractObjectName(task.getResultImageUrl());
        if (StringUtils.isNotBlank(resultObjectName)
                && !resultObjectName.equals(sourceObjectName)
                && isOwnedObject(userId, resultObjectName)) {
            // result 图为单次优化产物，未共享，直接清理
            pendingRemoval.add(resultObjectName);
        }

        minioStorageService.removeAfterCommit(minioStorageService.getBucketAvatars(), pendingRemoval);
    }

    /**
     * 清理指定简历关联的所有头像任务及 MinIO 文件。
     * <p>
     * 任务记录在当前事务内逻辑删除；MinIO 文件延迟到事务提交后移除。
     * 删除前检查共享引用：若同一源图仍被该用户其他未删除任务引用（如多份简历
     * 复用同一张自拍照），则跳过物理删除，避免跨简历误删。
     * </p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void cleanupTasksByResume(String userId, String resumeId) {
        LambdaQueryWrapper<AvatarTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AvatarTask::getUserId, userId)
                .eq(AvatarTask::getResumeId, resumeId);
        List<AvatarTask> tasks = avatarTaskMapper.selectList(wrapper);
        if (!tasks.isEmpty()) {
            avatarTaskMapper.delete(wrapper);
        }
        // 逻辑删除后再统计剩余引用：selectCount 自动追加 deleted=0，
        // 只统计本次清理之外仍存活的引用
        List<String> pendingRemoval = new ArrayList<>();
        tasks.stream()
                .map(AvatarTask::getSourceImageUrl)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .forEach(sourceImageUrl -> {
                    String objectName = extractObjectName(sourceImageUrl);
                    if (objectName == null || !isOwnedObject(userId, objectName)) {
                        return;
                    }
                    if (hasOtherActiveReference(userId, sourceImageUrl)) {
                        log.info("Skip removing shared avatar source, still referenced: userId={}, object={}",
                                userId, objectName);
                        return;
                    }
                    pendingRemoval.add(objectName);
                });
        minioStorageService.removeAfterCommit(minioStorageService.getBucketAvatars(), pendingRemoval);
    }

    /**
     * 判断该用户名下是否仍有未删除的任务引用同一源图 URL。
     */
    private boolean hasOtherActiveReference(String userId, String sourceImageUrl) {
        LambdaQueryWrapper<AvatarTask> refWrapper = new LambdaQueryWrapper<>();
        refWrapper.eq(AvatarTask::getUserId, userId)
                .eq(AvatarTask::getSourceImageUrl, sourceImageUrl);
        return avatarTaskMapper.selectCount(refWrapper) > 0;
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

    /**
     * 校验头像 URL 属于当前用户（对象路径第一段必须精确等于 userId）。
     */
    private void assertOwnAvatarUrl(String userId, String sourceImageUrl) {
        String objectName = extractObjectName(sourceImageUrl);
        if (!isOwnedObject(userId, objectName)) {
            throw new BusinessException(ResultCode.ACCESS_DENIED, "无权引用该头像资源。");
        }
    }

    /**
     * 判断对象路径是否属于当前用户，防止跨用户删除 MinIO 对象。
     * 精确比较第一段（不以 startsWith 前缀判断，避免等长 ID 之外的前缀误判）。
     */
    private boolean isOwnedObject(String userId, String objectName) {
        if (objectName == null || StringUtils.isBlank(userId)) {
            return false;
        }
        int idx = objectName.indexOf('/');
        String owner = idx < 0 ? objectName : objectName.substring(0, idx);
        return userId.equals(owner);
    }

    private String validateAvatarFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.AVATAR_FILE_EMPTY, "请上传头像图片。");
        }
        if (!Arrays.asList(ALLOWED_CONTENT_TYPES).contains(file.getContentType())) {
            throw new BusinessException(ResultCode.AVATAR_FORMAT_UNSUPPORTED, "请上传 JPG、PNG 或 WEBP 格式图片。");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ResultCode.AVATAR_FILE_TOO_LARGE, "图片大小不能超过 10MB。");
        }
        String format = detectImageFormat(file);
        if (format == null) {
            throw new BusinessException(ResultCode.AVATAR_FORMAT_UNSUPPORTED, "图片内容校验失败，请上传 JPG、PNG 或 WEBP 格式图片。");
        }
        return format;
    }

    /**
     * 通过文件头魔数识别真实图片格式，返回 jpg/png/webp；非法返回 null。
     * 防止伪造 Content-Type 上传任意内容。
     * 使用 file.getBytes() 避免 InputStream 12 字节消耗问题（见 ImageMagicUtil）。
     */
    private String detectImageFormat(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            String format = ImageMagicUtil.detectFormat(bytes.length >= 12 ? java.util.Arrays.copyOf(bytes, 12) : bytes);
            // 头像仅允许 jpg/png/webp，gif 等其它格式视为不支持
            return "jpg".equals(format) || "png".equals(format) || "webp".equals(format) ? format : null;
        } catch (IOException e) {
            return null;
        }
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
