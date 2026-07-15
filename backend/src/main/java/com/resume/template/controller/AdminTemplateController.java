package com.resume.template.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.resume.common.entity.R;
import com.resume.template.dto.AdminTemplateListItemResponse;
import com.resume.template.dto.AdminTemplateRequest;
import com.resume.template.dto.TemplateDTO;
import com.resume.template.service.TemplateService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 后台模板管理接口。
 */
@Validated
@RestController
@RequestMapping("/admin/templates")
@RequiredArgsConstructor
public class AdminTemplateController {

    private final TemplateService templateService;

    @GetMapping
    public R<Page<AdminTemplateListItemResponse>> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category) {
        return R.success(templateService.listAdminTemplates(page, size, status, category));
    }

    @GetMapping("/{id}")
    public R<TemplateDTO> get(@PathVariable String id) {
        return R.success(templateService.getAdminTemplate(id));
    }

    @PostMapping
    public R<TemplateDTO> create(@AuthenticationPrincipal String userId,
                                  @Valid @RequestBody AdminTemplateRequest request) {
        return R.success(templateService.createTemplate(request, userId));
    }

    @PutMapping("/{id}")
    public R<TemplateDTO> update(@PathVariable String id,
                                  @Valid @RequestBody AdminTemplateRequest request) {
        return R.success(templateService.updateTemplate(id, request));
    }

    @PatchMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable String id,
                                @RequestBody Map<String, String> body) {
        templateService.updateTemplateStatus(id, body.get("status"));
        return R.success();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable String id) {
        templateService.deleteTemplate(id);
        return R.success();
    }
}
