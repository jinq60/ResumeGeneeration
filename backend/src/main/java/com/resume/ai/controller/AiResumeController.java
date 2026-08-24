package com.resume.ai.controller;

import com.resume.ai.dto.ResumeOptimizeRequest;
import com.resume.ai.dto.ResumeOptimizeResponse;
import com.resume.ai.entity.ResumeOptimizeTask;
import com.resume.ai.service.AiResumeOptimizeService;
import com.resume.common.constant.BizConstant;
import com.resume.common.entity.R;
import com.resume.resume.entity.Resume;
import com.resume.resume.service.ResumeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * AI 功能 REST 控制器。
 */
@Slf4j
@RestController
@RequestMapping("/resumes")
@RequiredArgsConstructor
public class AiResumeController {

    private final ResumeService resumeService;
    private final AiResumeOptimizeService aiResumeOptimizeService;

    /**
     * 创建 JD 匹配优化任务。
     */
    @PostMapping("/{resumeId}/optimize")
    public R<ResumeOptimizeResponse> createOptimize(@AuthenticationPrincipal String userId,
                                                     @PathVariable String resumeId,
                                                     @Valid @RequestBody ResumeOptimizeRequest request) {
        Resume resume = resumeService.getResumeEntity(userId, resumeId);

        ResumeOptimizeTask task = aiResumeOptimizeService.createTask(
                userId, resumeId, resume, request.getJobDescription());

        try {
            aiResumeOptimizeService.executeOptimize(task.getId(), resume, request.getJobDescription());
        } catch (org.springframework.core.task.TaskRejectedException e) {
            // AI 线程池队列已满：任务无法入队，标记失败并正常返回，由前端轮询感知失败
            log.warn("AI optimize task rejected (thread pool exhausted): taskId={}", task.getId());
            aiResumeOptimizeService.markTaskRejected(task.getId());
        }

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
        ResumeOptimizeTask task = aiResumeOptimizeService.getOwnedTask(userId, resumeId, taskId);
        return R.success(toResponse(task));
    }

    /**
     * 获取最新优化结果。
     */
    @GetMapping("/{resumeId}/optimize/latest")
    public R<ResumeOptimizeResponse> getLatestOptimize(@AuthenticationPrincipal String userId,
                                                        @PathVariable String resumeId) {
        ResumeOptimizeTask task = aiResumeOptimizeService.getLatestTask(userId, resumeId);
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
