package com.resume.common.controller;

import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.common.service.MinioStorageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * MinIO 文件代理 —— 将 /uploads/avatars/* 和 /uploads/templates/* 请求转发到 MinIO。
 */
@RestController
@RequiredArgsConstructor
public class StaticResourceController {

    private final MinioStorageService minioStorageService;

    @GetMapping("/uploads/avatars/**")
    public ResponseEntity<byte[]> serveAvatar(HttpServletRequest request) {
        String objectName = extractObjectName(request, "/uploads/avatars/");
        validateObjectName(objectName);

        byte[] data = minioStorageService.download(minioStorageService.getBucketAvatars(), objectName);

        return buildResponse(objectName, data, false);
    }

    @GetMapping("/uploads/templates/**")
    public ResponseEntity<byte[]> serveTemplate(HttpServletRequest request) {
        String objectName = extractObjectName(request, "/uploads/templates/");
        validateObjectName(objectName);

        byte[] data = minioStorageService.download(minioStorageService.getBucketTemplates(), objectName);

        return buildResponse(objectName, data, true);
    }

    /**
     * 内置模板缩略图（classpath:static/templates/*.svg）。
     * <p>
     * 路径使用 /templates/thumbs/** 前缀，避免与 TemplateController 的 /templates/{id} 冲突。
     * </p>
     */
    @GetMapping("/templates/thumbs/**")
    public ResponseEntity<byte[]> serveTemplateThumb(HttpServletRequest request) {
        String fileName = extractObjectName(request, "/templates/thumbs/");
        validateObjectName(fileName);

        org.springframework.core.io.ClassPathResource resource =
                new org.springframework.core.io.ClassPathResource("static/templates/" + fileName);
        if (!resource.exists()) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "缩略图不存在。");
        }
        try (java.io.InputStream in = resource.getInputStream()) {
            return buildResponse(fileName, in.readAllBytes(), true);
        } catch (java.io.IOException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "缩略图读取失败。");
        }
    }

    private String extractObjectName(HttpServletRequest request, String prefix) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String fullPrefix = (contextPath != null ? contextPath : "") + prefix;
        if (uri.startsWith(fullPrefix)) {
            return uri.substring(fullPrefix.length());
        }
        // 兼容测试环境无 context-path 的情况
        if (uri.startsWith(prefix)) {
            return uri.substring(prefix.length());
        }
        return "";
    }

    private void validateObjectName(String objectName) {
        if (StringUtils.isBlank(objectName) || objectName.contains("..") || objectName.startsWith("/")) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "文件路径不合法。");
        }
    }

    private ResponseEntity<byte[]> buildResponse(String objectName, byte[] data, boolean cacheable) {
        String contentType = guessContentType(objectName);
        org.springframework.http.ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(cacheable
                        ? CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic()
                        : CacheControl.noStore());
        return builder.body(data);
    }

    private String guessContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif")) return "image/gif";
        // HTML/JS/SVG 等可执行内容一律按二进制返回，防止同源存储型 XSS
        return "application/octet-stream";
    }
}
