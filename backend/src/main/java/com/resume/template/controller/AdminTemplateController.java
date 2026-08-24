package com.resume.template.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.resume.common.entity.R;
import com.resume.common.service.MinioStorageService;
import com.resume.common.util.ImageMagicUtil;
import com.resume.template.dto.AdminTemplateListItemResponse;
import com.resume.template.dto.AdminTemplateRequest;
import com.resume.template.dto.TemplateDTO;
import com.resume.template.dto.TemplateStatsResponse;
import com.resume.template.service.TemplateService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/**
 * 后台模板管理接口。
 */
@Validated
@RestController
@RequestMapping("/admin/templates")
@RequiredArgsConstructor
public class AdminTemplateController {

    private final TemplateService templateService;
    private final MinioStorageService minioStorageService;

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

    @GetMapping("/stats")
    public R<TemplateStatsResponse> stats() {
        return R.success(templateService.stats());
    }

    @PostMapping("/upload")
    public R<Map<String, String>> uploadThumbnail(@RequestParam("file") MultipartFile file) {
        String format = validateThumbnailFile(file);

        // 扩展名与 Content-Type 均由魔数识别结果决定，避免伪造文件名/Content-Type 造成后缀与内容不一致
        String ext = "." + format;
        String contentType = ImageMagicUtil.toContentType(format);
        String objectName = "templates/" + UUID.randomUUID() + ext;
        try {
            minioStorageService.upload(minioStorageService.getBucketTemplates(), objectName,
                    file.getInputStream(), file.getSize(), contentType);
        } catch (java.io.IOException e) {
            throw new com.resume.common.exception.BusinessException(com.resume.common.constant.ResultCode.INTERNAL_ERROR, "读取文件失败");
        }
        return R.success(Map.of("url", "/uploads/templates/" + objectName));
    }

    private static final java.util.Set<String> ALLOWED_THUMB_FORMATS =
            java.util.Set.of("jpg", "png", "webp");
    private static final long MAX_THUMB_SIZE = 5 * 1024 * 1024;

    private String validateThumbnailFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new com.resume.common.exception.BusinessException(com.resume.common.constant.ResultCode.PARAM_INVALID, "请上传缩略图文件。");
        }
        if (file.getSize() > MAX_THUMB_SIZE) {
            throw new com.resume.common.exception.BusinessException(com.resume.common.constant.ResultCode.PARAM_INVALID, "缩略图大小不能超过 5MB。");
        }
        String format;
        try (java.io.InputStream in = file.getInputStream()) {
            format = ImageMagicUtil.detectFormat(in);
        } catch (java.io.IOException e) {
            throw new com.resume.common.exception.BusinessException(com.resume.common.constant.ResultCode.PARAM_INVALID, "图片内容校验失败。");
        }
        if (format == null || !ALLOWED_THUMB_FORMATS.contains(format)) {
            throw new com.resume.common.exception.BusinessException(com.resume.common.constant.ResultCode.PARAM_INVALID, "仅支持 JPG、PNG、WEBP 图片。");
        }
        return format;
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

    /**
     * 导出模板 JSON（含配置与 HTML 模板引用），便于迁移到其他环境。
     */
    @GetMapping("/{id}/export")
    public void export(@PathVariable String id, jakarta.servlet.http.HttpServletResponse response)
            throws java.io.IOException {
        TemplateDTO template = templateService.getAdminTemplate(id);
        String json = new com.fasterxml.jackson.databind.ObjectMapper()
                .writerWithDefaultPrettyPrinter().writeValueAsString(template);
        String fileName = java.net.URLEncoder.encode(
                template.getName() + "_" + template.getCode() + ".json", StandardCharsets.UTF_8)
                .replace("+", "%20");
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);
        response.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
    }
}
