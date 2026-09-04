package com.resume.resume.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.resume.common.entity.R;
import com.resume.resume.dto.*;
import com.resume.resume.service.ResumeReviewService;
import com.resume.resume.service.ResumeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 简历相关接口。
 */
@Validated
@RestController
@RequestMapping("/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;
    private final ResumeReviewService resumeReviewService;
    private final com.resume.resume.service.ResumeImportService resumeImportService;

    @PostMapping
    public R<ResumeDetailResponse> create(@AuthenticationPrincipal String userId,
                                          @Valid @RequestBody CreateResumeRequest request) {
        return R.success(resumeService.createResume(userId, request));
    }

    @PostMapping("/import")
    public R<ResumeDetailResponse> importResume(@AuthenticationPrincipal String userId,
                                                @Valid @RequestBody com.resume.resume.dto.ResumeImportRequest request) {
        return R.success(resumeImportService.importResume(userId, request));
    }

    @GetMapping
    public R<Page<ResumeListItemResponse>> list(@AuthenticationPrincipal String userId,
                                                 @RequestParam(defaultValue = "1") @Min(1) int page,
                                                 @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
                                                 @RequestParam(required = false) @jakarta.validation.constraints.Size(max = 64) String keyword,
                                                 @RequestParam(required = false) @jakarta.validation.constraints.Size(max = 64) String scene,
                                                 @RequestParam(required = false) @jakarta.validation.constraints.Size(max = 64) String targetPosition) {
        return R.success(resumeService.listResumes(userId, page, size, keyword, scene, targetPosition));
    }

    @GetMapping("/{id}")
    public R<ResumeDetailResponse> get(@AuthenticationPrincipal String userId,
                                        @PathVariable String id) {
        return R.success(resumeService.getResume(userId, id));
    }

    @PutMapping("/{id}")
    public R<UpdateResumeResponse> update(@AuthenticationPrincipal String userId,
                                               @PathVariable String id,
                                               @Valid @RequestBody UpdateResumeRequest request) {
        return R.success(resumeService.updateResume(userId, id, request));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@AuthenticationPrincipal String userId,
                          @PathVariable String id) {
        resumeService.deleteResume(userId, id);
        return R.success();
    }

    @PostMapping("/{id}/duplicate")
    public R<DuplicateResumeResponse> duplicate(@AuthenticationPrincipal String userId,
                                              @PathVariable String id) {
        return R.success(resumeService.duplicateResume(userId, id));
    }

    @PutMapping("/{id}/title")
    public R<RenameResumeResponse> rename(@AuthenticationPrincipal String userId,
                                          @PathVariable String id,
                                          @Valid @RequestBody RenameResumeRequest request) {
        return R.success(resumeService.renameResume(userId, id, request));
    }

    @PostMapping("/{id}/reviews")
    public R<ResumeReviewResponse> review(@AuthenticationPrincipal String userId,
                                          @PathVariable String id,
                                          @Valid @RequestBody ReviewResumeRequest request) {
        return R.success(resumeReviewService.reviewResume(userId, id, request));
    }

    @GetMapping("/{id}/reviews/latest")
    public R<ResumeReviewResponse> latestReview(@AuthenticationPrincipal String userId,
                                                @PathVariable String id) {
        return R.success(resumeReviewService.getLatestReview(userId, id));
    }
}
