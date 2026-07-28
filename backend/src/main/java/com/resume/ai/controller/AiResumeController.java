package com.resume.ai.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.resume.ai.dto.ResumeOptimizeRequest;
import com.resume.ai.dto.ResumeOptimizeResponse;
import com.resume.ai.entity.ResumeOptimizeTask;
import com.resume.ai.mapper.ResumeOptimizeTaskMapper;
import com.resume.ai.service.AiResumeOptimizeService;
import com.resume.common.constant.BizConstant;
import com.resume.common.constant.ResultCode;
import com.resume.common.entity.R;
import com.resume.common.exception.BusinessException;
import com.resume.resume.entity.Resume;
import com.resume.resume.service.ResumeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 功能 REST 控制器。
 */
@RestController
@RequestMapping("/resumes")
@RequiredArgsConstructor
public class AiResumeController {

    private final ResumeService resumeService;
    private final ResumeOptimizeTaskMapper optimizeTaskMapper;
    private final AiResumeOptimizeService aiResumeOptimizeService;

    private static final int MAX_CONCURRENT_PER_USER = 3;

    /**
     * 创建 JD 匹配优化任务。
     */
    @PostMapping("/{resumeId}/optimize")
    public R<ResumeOptimizeResponse> createOptimize(@AuthenticationPrincipal String userId,
                                                     @PathVariable String resumeId,
                                                     @Valid @RequestBody ResumeOptimizeRequest request) {
        Resume resume = resumeService.getResumeEntity(userId, resumeId);

        long runningCount = optimizeTaskMapper.selectCount(
                new LambdaQueryWrapper<ResumeOptimizeTask>()
                        .eq(ResumeOptimizeTask::getUserId, userId)
                        .in(ResumeOptimizeTask::getStatus, List.of(
                                BizConstant.TASK_STATUS_PENDING, BizConstant.TASK_STATUS_PROCESSING)));
        if (runningCount >= MAX_CONCURRENT_PER_USER) {
            throw new BusinessException(ResultCode.AI_CONCURRENT_LIMIT_EXCEEDED,
                    "同时进行的 AI 任务过多，请等待当前任务完成后再试。");
        }

        ResumeOptimizeTask task = new ResumeOptimizeTask();
        task.setResumeId(resumeId);
        task.setUserId(userId);
        task.setJobDescription(request.getJobDescription());
        task.setStatus(BizConstant.TASK_STATUS_PENDING);
        task.setDeleted(BizConstant.NOT_DELETED);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        optimizeTaskMapper.insert(task);

        aiResumeOptimizeService.executeOptimize(task.getId(), resume, request.getJobDescription());

        ResumeOptimizeResponse response = new ResumeOptimizeResponse();
        response.setTaskId(task.getId());
        response.setResumeId(resumeId);
        response.setStatus(BizConstant.TASK_STATUS_PENDING);
        response.setCreatedAt(task.getCreatedAt());
        return R.success(response);
    }

    /**
     * 查询 JD 匹配优化任务结果。
     */
    @GetMapping("/{resumeId}/optimize/{taskId}")
    public R<ResumeOptimizeResponse> getOptimizeResult(@AuthenticationPrincipal String userId,
                                                        @PathVariable String resumeId,
                                                        @PathVariable String taskId) {
        ResumeOptimizeTask task = optimizeTaskMapper.selectById(taskId);
        if (task == null || BizConstant.DELETED.equals(task.getDeleted())) {
            throw new BusinessException(ResultCode.AI_TASK_NOT_FOUND, "优化任务不存在。");
        }
        if (!userId.equals(task.getUserId())) {
            throw new BusinessException(ResultCode.ACCESS_DENIED, "无权访问该资源。");
        }
        return R.success(toResponse(task));
    }

    /**
     * 获取最新优化结果。
     */
    @GetMapping("/{resumeId}/optimize/latest")
    public R<ResumeOptimizeResponse> getLatestOptimize(@AuthenticationPrincipal String userId,
                                                        @PathVariable String resumeId) {
        LambdaQueryWrapper<ResumeOptimizeTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResumeOptimizeTask::getResumeId, resumeId)
                .eq(ResumeOptimizeTask::getUserId, userId)
                .eq(ResumeOptimizeTask::getStatus, BizConstant.TASK_STATUS_SUCCESS)
                .eq(ResumeOptimizeTask::getDeleted, BizConstant.NOT_DELETED)
                .orderByDesc(ResumeOptimizeTask::getCreatedAt)
                .last("LIMIT 1");
        ResumeOptimizeTask task = optimizeTaskMapper.selectOne(wrapper);
        if (task == null) {
            return R.success(null);
        }
        return R.success(toResponse(task));
    }

    private ResumeOptimizeResponse toResponse(ResumeOptimizeTask task) {
        ResumeOptimizeResponse response = new ResumeOptimizeResponse();
        response.setTaskId(task.getId());
        response.setResumeId(task.getResumeId());
        response.setStatus(task.getStatus());
        response.setMatchScore(task.getMatchScore());
        response.setDimensionScores(task.getDimensionScores());
        response.setOptimizations(task.getOptimizations());
        response.setMissingSkills(task.getMissingSkills());
        response.setRecommendations(task.getRecommendations());
        response.setErrorMsg(task.getErrorMsg());
        response.setCreatedAt(task.getCreatedAt());
        return response;
    }
}
