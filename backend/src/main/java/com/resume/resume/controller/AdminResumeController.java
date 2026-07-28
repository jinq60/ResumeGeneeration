package com.resume.resume.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.resume.common.entity.R;
import com.resume.resume.dto.AdminResumeListItemResponse;
import com.resume.resume.dto.AdminResumeStatsResponse;
import com.resume.resume.service.ResumeService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 后台简历管理接口（需 ADMIN 角色）。
 */
@Validated
@RestController
@RequestMapping("/admin/resumes")
@RequiredArgsConstructor
public class AdminResumeController {

    private final ResumeService resumeService;

    @GetMapping
    public R<Page<AdminResumeListItemResponse>> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String keyword) {
        return R.success(resumeService.listAdminResumes(page, size, keyword));
    }

    @GetMapping("/stats")
    public R<AdminResumeStatsResponse> stats() {
        return R.success(resumeService.adminStats());
    }
}
