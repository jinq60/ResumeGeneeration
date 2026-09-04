package com.resume.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.ai.config.AiPromptTemplates;
import com.resume.ai.config.AiProperties;
import com.resume.ai.dto.AiChatRequest;
import com.resume.ai.dto.AiChatResponse;
import com.resume.ai.dto.ResumeOptimizeResponse.SectionOptimization;
import com.resume.ai.entity.AiCallLog;
import com.resume.ai.entity.ResumeOptimizeTask;
import com.resume.ai.mapper.AiCallLogMapper;
import com.resume.ai.mapper.ResumeOptimizeTaskMapper;
import com.resume.ai.provider.LlmProvider;
import com.resume.ai.provider.ProviderRouter;
import com.resume.ai.task.AiZombieTaskSweeper;
import com.resume.ai.util.AiCallLogDefaults;
import com.resume.common.constant.BizConstant;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.notification.service.NotificationService;
import com.resume.resume.dto.SectionDTO;
import com.resume.resume.entity.Resume;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
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
    private static final int MAX_CONCURRENT_PER_USER = 3;
    private final java.util.concurrent.ConcurrentHashMap<String, Object> optimizeLocks = new java.util.concurrent.ConcurrentHashMap<>();

    private final ResumeOptimizeTaskMapper optimizeTaskMapper;
    private final AiCallLogMapper aiCallLogMapper;
    private final ProviderRouter providerRouter;
    private final AiPromptTemplates promptTemplates;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    /**
     * 创建优化任务（含每用户并发配额校验）。
     */
    public ResumeOptimizeTask createTask(String userId, String resumeId, Resume resume, String jobDescription) {
        Object lock = optimizeLocks.computeIfAbsent(userId, k -> new Object());
        try {
            synchronized (lock) {
                // 计数时排除 updated_at 超过阈值仍 pending/processing 的僵尸行，
                // 避免服务重启后的滞留任务把用户并发额度永久占满；加锁防并发超卖
                long runningCount = optimizeTaskMapper.selectCount(
                        new LambdaQueryWrapper<ResumeOptimizeTask>()
                                .eq(ResumeOptimizeTask::getUserId, userId)
                                .in(ResumeOptimizeTask::getStatus, List.of(
                                        BizConstant.TASK_STATUS_PENDING, BizConstant.TASK_STATUS_PROCESSING))
                                .ge(ResumeOptimizeTask::getUpdatedAt,
                                        LocalDateTime.now().minus(AiZombieTaskSweeper.ZOMBIE_THRESHOLD)));
                if (runningCount >= MAX_CONCURRENT_PER_USER) {
                    throw new BusinessException(ResultCode.AI_CONCURRENT_LIMIT_EXCEEDED,
                            "同时进行的 AI 任务过多，请等待当前任务完成后再试。");
                }

                ResumeOptimizeTask task = new ResumeOptimizeTask();
                task.setResumeId(resumeId);
                task.setUserId(userId);
                task.setJobDescription(jobDescription);
                task.setStatus(BizConstant.TASK_STATUS_PENDING);
                task.setDeleted(BizConstant.NOT_DELETED);
                task.setCreatedAt(LocalDateTime.now());
                task.setUpdatedAt(LocalDateTime.now());
                optimizeTaskMapper.insert(task);
                return task;
            }
        } finally {
            // 不 remove：remove 会引入 ABA（A 持 L1、B 阻塞 L1、A 删 L1、C 建 L2 与 B 并发），
            // 突破 MAX_CONCURRENT 限流。用户量有界时锁对象常驻可接受；多实例需 Redis NX。
        }
    }

    /**
     * 查询任务并校验归属与简历一致性。
     */
    public ResumeOptimizeTask getOwnedTask(String userId, String resumeId, String taskId) {
        ResumeOptimizeTask task = optimizeTaskMapper.selectById(taskId);
        if (task == null || BizConstant.DELETED.equals(task.getDeleted())) {
            throw new BusinessException(ResultCode.AI_TASK_NOT_FOUND, "优化任务不存在。");
        }
        if (!userId.equals(task.getUserId())) {
            throw new BusinessException(ResultCode.ACCESS_DENIED, "无权访问该资源。");
        }
        if (!resumeId.equals(task.getResumeId())) {
            throw new BusinessException(ResultCode.ACCESS_DENIED, "任务与简历不匹配。");
        }
        return task;
    }

    /**
     * 获取指定简历的最新成功优化结果。
     */
    public ResumeOptimizeTask getLatestTask(String userId, String resumeId) {
        LambdaQueryWrapper<ResumeOptimizeTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResumeOptimizeTask::getResumeId, resumeId)
                .eq(ResumeOptimizeTask::getUserId, userId)
                .eq(ResumeOptimizeTask::getStatus, BizConstant.TASK_STATUS_SUCCESS)
                .eq(ResumeOptimizeTask::getDeleted, BizConstant.NOT_DELETED)
                .orderByDesc(ResumeOptimizeTask::getCreatedAt)
                .last("LIMIT 1");
        return optimizeTaskMapper.selectOne(wrapper);
    }

    /**
     * 线程池队列已满、异步任务被拒绝时的补偿：将任务标记为 failed，让用户可感知。
     */
    public void markTaskRejected(String taskId) {
        LambdaUpdateWrapper<ResumeOptimizeTask> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ResumeOptimizeTask::getId, taskId)
                .in(ResumeOptimizeTask::getStatus, List.of(BizConstant.TASK_STATUS_PENDING, BizConstant.TASK_STATUS_PROCESSING))
                .set(ResumeOptimizeTask::getStatus, BizConstant.TASK_STATUS_FAILED)
                .set(ResumeOptimizeTask::getErrorMsg, "AI 服务繁忙，请稍后再试。")
                .set(ResumeOptimizeTask::getUpdatedAt, LocalDateTime.now());
        optimizeTaskMapper.update(null, wrapper);
    }

    @Async("aiTaskExecutor")
    public void executeOptimize(String taskId, Resume resume, String jobDescription) {
        ResumeOptimizeTask task = optimizeTaskMapper.selectById(taskId);
        if (task == null) {
            log.error("Optimize task not found: {}", taskId);
            return;
        }

        // CAS：仅 pending 可转 processing；若 sweeper 已置 failed 则直接返回不再复活
        LambdaUpdateWrapper<ResumeOptimizeTask> processingGuard = new LambdaUpdateWrapper<>();
        processingGuard.eq(ResumeOptimizeTask::getId, taskId)
                .eq(ResumeOptimizeTask::getStatus, BizConstant.TASK_STATUS_PENDING)
                .set(ResumeOptimizeTask::getStatus, BizConstant.TASK_STATUS_PROCESSING)
                .set(ResumeOptimizeTask::getUpdatedAt, LocalDateTime.now());
        Integer processingAffected = optimizeTaskMapper.update(null, processingGuard);
        if (processingAffected == null || processingAffected == 0) {
            log.warn("Optimize task processing CAS missed (swept?): taskId={}", taskId);
            return;
        }
        task.setStatus(BizConstant.TASK_STATUS_PROCESSING);

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

            AiProperties.FeatureConfig featureConfig = providerRouter.getFeatureConfig(FEATURE_KEY);
            AiChatRequest request = AiChatRequest.builder()
                    .model(model)
                    .userPrompt(prompt)
                    .temperature(0.3)
                    .maxTokens(4096)
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
            // CAS：仅 pending/processing 可转终态，避免覆盖 sweeper 已置 failed
            LambdaUpdateWrapper<ResumeOptimizeTask> finalGuard = new LambdaUpdateWrapper<>();
            finalGuard.eq(ResumeOptimizeTask::getId, taskId)
                    .in(ResumeOptimizeTask::getStatus, List.of(BizConstant.TASK_STATUS_PENDING, BizConstant.TASK_STATUS_PROCESSING));
            if (BizConstant.TASK_STATUS_SUCCESS.equals(task.getStatus())) {
                finalGuard.set(ResumeOptimizeTask::getStatus, BizConstant.TASK_STATUS_SUCCESS)
                        .set(ResumeOptimizeTask::getCompletedAt, task.getCompletedAt())
                        .set(ResumeOptimizeTask::getUpdatedAt, task.getUpdatedAt());
            } else {
                finalGuard.set(ResumeOptimizeTask::getStatus, BizConstant.TASK_STATUS_FAILED)
                        .set(ResumeOptimizeTask::getErrorMsg, task.getErrorMsg())
                        .set(ResumeOptimizeTask::getUpdatedAt, task.getUpdatedAt());
            }
            // 结果明细（优化内容）仍需落库：先 CAS 状态，成功后再按 id 更新业务字段（此时状态已为终态，不再竞争）
            Integer finalAffected = optimizeTaskMapper.update(null, finalGuard);
            if (finalAffected == null || finalAffected == 0) {
                log.warn("Optimize task final CAS missed (swept?): taskId={}", taskId);
            } else if (BizConstant.TASK_STATUS_SUCCESS.equals(task.getStatus())) {
                ResumeOptimizeTask detail = new ResumeOptimizeTask();
                detail.setId(taskId);
                detail.setMatchScore(task.getMatchScore());
                detail.setDimensionScores(task.getDimensionScores());
                detail.setOptimizations(task.getOptimizations());
                detail.setMissingSkills(task.getMissingSkills());
                detail.setRecommendations(task.getRecommendations());
                detail.setModelName(task.getModelName());
                optimizeTaskMapper.updateById(detail);
            }
            notifyTaskResult(task, resume);
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
     * 任务终态后推送站内通知（完成 / 失败）。
     */
    private void notifyTaskResult(ResumeOptimizeTask task, Resume resume) {
        try {
            if (resume.getUserId() == null) {
                return;
            }
            if (BizConstant.TASK_STATUS_SUCCESS.equals(task.getStatus())) {
                notificationService.notify(resume.getUserId(), "ai", "JD 优化完成",
                        "你的简历《" + resume.getTitle() + "》JD 匹配优化已完成，综合匹配分 "
                                + task.getMatchScore() + "，快去查看优化建议吧。");
            } else if (BizConstant.TASK_STATUS_FAILED.equals(task.getStatus())) {
                String reason = org.apache.commons.lang3.StringUtils.abbreviate(
                        org.apache.commons.lang3.StringUtils.defaultString(task.getErrorMsg(), "未知原因"), 100);
                notificationService.notify(resume.getUserId(), "ai", "JD 优化失败",
                        "你的简历《" + resume.getTitle() + "》JD 匹配优化失败：" + reason + "，请稍后重试。");
            }
        } catch (Exception ex) {
            log.warn("Notify optimize task result failed: taskId={}", task.getId(), ex);
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
