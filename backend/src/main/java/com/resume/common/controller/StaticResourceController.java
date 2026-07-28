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

        return buildResponse(objectName, data);
    }

    @GetMapping("/uploads/templates/**")
    public ResponseEntity<byte[]> serveTemplate(HttpServletRequest request) {
        String objectName = extractObjectName(request, "/uploads/templates/");
        validateObjectName(objectName);

        byte[] data = minioStorageService.download(minioStorageService.getBucketTemplates(), objectName);

        return buildResponse(objectName, data);
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

    private ResponseEntity<byte[]> buildResponse(String objectName, byte[] data) {
        String contentType = guessContentType(objectName);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(data);
    }

    private String guessContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".html")) return "text/html";
        return "application/octet-stream";
    }
}
