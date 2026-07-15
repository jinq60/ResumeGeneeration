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
 * MinIO 文件代理 —— 将 /uploads/avatars/* 请求转发到 MinIO。
 */
@RestController
@RequiredArgsConstructor
public class StaticResourceController {

    private final MinioStorageService minioStorageService;

    @GetMapping("/uploads/avatars/**")
    public ResponseEntity<byte[]> serveAvatar(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // uri: /api/uploads/avatars/{userId}/avatars/{file}
        String objectName = extractObjectName(request.getContextPath(), uri);
        if (StringUtils.isBlank(objectName) || objectName.contains("..") || objectName.startsWith("/")) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "文件路径不合法。");
        }

        byte[] data = minioStorageService.download(minioStorageService.getBucketAvatars(), objectName);

        String contentType = guessContentType(objectName);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(data);
    }

    private String extractObjectName(String contextPath, String uri) {
        String prefix = (contextPath != null ? contextPath : "") + "/uploads/avatars/";
        if (uri.startsWith(prefix)) {
            return uri.substring(prefix.length());
        }
        // 兼容测试环境无 context-path 的情况
        String fallback = "/uploads/avatars/";
        if (uri.startsWith(fallback)) {
            return uri.substring(fallback.length());
        }
        return "";
    }

    private String guessContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif")) return "image/gif";
        return "application/octet-stream";
    }
}
