package com.resume.template.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.resume.common.entity.R;
import com.resume.common.service.MinioStorageService;
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
        validateThumbnailFile(file);

        String ext = getExtension(file.getOriginalFilename());
        String objectName = "templates/" + UUID.randomUUID() + ext;
        try {
            minioStorageService.upload(minioStorageService.getBucketTemplates(), objectName,
                    file.getInputStream(), file.getSize(), file.getContentType());
        } catch (java.io.IOException e) {
            throw new com.resume.common.exception.BusinessException(com.resume.common.constant.ResultCode.INTERNAL_ERROR, "读取文件失败");
        }
        return R.success(Map.of("url", "/uploads/templates/" + objectName));
    }

    private static final java.util.Set<String> ALLOWED_THUMB_EXTENSIONS =
            java.util.Set.of(".jpg", ".jpeg", ".png", ".webp");
    private static final long MAX_THUMB_SIZE = 5 * 1024 * 1024;

    private void validateThumbnailFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new com.resume.common.exception.BusinessException(com.resume.common.constant.ResultCode.PARAM_INVALID, "请上传缩略图文件。");
        }
        if (file.getSize() > MAX_THUMB_SIZE) {
            throw new com.resume.common.exception.BusinessException(com.resume.common.constant.ResultCode.PARAM_INVALID, "缩略图大小不能超过 5MB。");
        }
        String ext = getExtension(file.getOriginalFilename());
        if (!ALLOWED_THUMB_EXTENSIONS.contains(ext)) {
            throw new com.resume.common.exception.BusinessException(com.resume.common.constant.ResultCode.PARAM_INVALID, "仅支持 JPG、PNG、WEBP 图片。");
        }
        if (!isValidImageMagic(file)) {
            throw new com.resume.common.exception.BusinessException(com.resume.common.constant.ResultCode.PARAM_INVALID, "图片内容校验失败。");
        }
    }

    private String getExtension(String filename) {
        if (filename == null) {
            return "";
        }
        String lower = filename.toLowerCase();
        int idx = lower.lastIndexOf('.');
        return idx >= 0 ? lower.substring(idx) : "";
    }

    private boolean isValidImageMagic(MultipartFile file) {
        try (java.io.InputStream in = file.getInputStream()) {
            byte[] header = in.readNBytes(12);
            if (header.length < 4) {
                return false;
            }
            if (header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF) {
                return true;
            }
            if ((header[0] & 0xFF) == 0x89 && header[1] == (byte) 0x50 && header[2] == (byte) 0x4E
                    && header[3] == (byte) 0x47) {
                return true;
            }
            return header[0] == (byte) 0x52 && header[1] == (byte) 0x49 && header[2] == (byte) 0x46
                    && header[3] == (byte) 0x46 && header.length >= 12
                    && header[8] == (byte) 0x57 && header[9] == (byte) 0x45
                    && header[10] == (byte) 0x42 && header[11] == (byte) 0x50;
        } catch (java.io.IOException e) {
            return false;
        }
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
