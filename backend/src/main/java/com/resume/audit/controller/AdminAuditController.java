package com.resume.audit.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.resume.audit.dto.AuditItemResponse;
import com.resume.audit.dto.AuditReviewRequest;
import com.resume.audit.dto.AuditStatsResponse;
import com.resume.audit.service.ContentAuditService;
import com.resume.common.entity.R;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端内容审核接口（需 ADMIN 角色）。
 */
@Validated
@RestController
@RequestMapping("/admin/audits")
@RequiredArgsConstructor
public class AdminAuditController {

    private final ContentAuditService contentAuditService;

    @GetMapping
    public R<Page<AuditItemResponse>> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String riskLevel) {
        return R.success(contentAuditService.list(page, size, keyword, status, riskLevel));
    }

    @GetMapping("/stats")
    public R<AuditStatsResponse> stats() {
        return R.success(contentAuditService.stats());
    }

    @PostMapping("/{id}/approve")
    public R<AuditItemResponse> approve(@AuthenticationPrincipal String reviewerId,
                                        @PathVariable String id,
                                        @RequestBody(required = false) @Valid AuditReviewRequest request) {
        return R.success(contentAuditService.review(reviewerId, id, "approve",
                request == null ? null : request.getNote(), null));
    }

    @PostMapping("/{id}/reject")
    public R<AuditItemResponse> reject(@AuthenticationPrincipal String reviewerId,
                                       @PathVariable String id,
                                       @RequestBody(required = false) @Valid AuditReviewRequest request) {
        return R.success(contentAuditService.review(reviewerId, id, "reject",
                request == null ? null : request.getNote(), null));
    }

    @PostMapping("/{id}/mark-warning")
    public R<AuditItemResponse> markWarning(@AuthenticationPrincipal String reviewerId,
                                            @PathVariable String id,
                                            @RequestBody(required = false) @Valid AuditReviewRequest request) {
        return R.success(contentAuditService.review(reviewerId, id, "warning",
                request == null ? null : request.getNote(),
                request == null ? null : request.getRiskLevel()));
    }
}