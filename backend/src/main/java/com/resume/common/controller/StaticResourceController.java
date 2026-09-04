package com.resume.common.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.common.service.MinioStorageService;
import com.resume.resume.share.entity.ResumeShare;
import com.resume.resume.share.mapper.ResumeShareMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 文件代理 —— 将 /uploads/avatars/* 和 /uploads/templates/* 请求转发到 MinIO。
 * 头像为隐私资源：已登录可直接访问；匿名需提供合法 shareToken（公开分享旁路）或来自 /share/ 的 Referer。
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class StaticResourceController {

    private final MinioStorageService minioStorageService;
    private final ResumeShareMapper resumeShareMapper;

    @GetMapping("/uploads/avatars/**")
    public ResponseEntity<org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody> serveAvatar(HttpServletRequest request) {
        String objectName = extractObjectName(request, "/uploads/avatars/");
        validateObjectName(objectName);
        enforceAvatarAccess(request, objectName);
        java.io.InputStream is = minioStorageService.downloadStream(minioStorageService.getBucketAvatars(), objectName);
        String contentType = guessContentType(objectName);
        org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody body = out -> {
            try (java.io.InputStream in = is) {
                in.transferTo(out);
            }
        };
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'none'; img-src 'self'; style-src 'unsafe-inline'")
                .cacheControl(org.springframework.http.CacheControl.noStore())
                .body(body);
    }

    @GetMapping("/uploads/templates/**")
    public ResponseEntity<org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody> serveTemplate(HttpServletRequest request) {
        String objectName = extractObjectName(request, "/uploads/templates/");
        validateObjectName(objectName);
        java.io.InputStream is = minioStorageService.downloadStream(minioStorageService.getBucketTemplates(), objectName);
        String contentType = guessContentType(objectName);
        org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody body = out -> {
            try (java.io.InputStream in = is) {
                in.transferTo(out);
            }
        };
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'none'; img-src 'self'; style-src 'unsafe-inline'")
                .cacheControl(org.springframework.http.CacheControl.maxAge(1, java.util.concurrent.TimeUnit.HOURS).cachePublic())
                .body(body);
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
            // 内置模板缩略图来自可信 classpath，SVG 可安全以 image/svg+xml 返回以正常显示
            String contentType = fileName.toLowerCase().endsWith(".svg")
                    ? "image/svg+xml"
                    : guessContentType(fileName);
            return buildResponse(fileName, in.readAllBytes(), true, contentType);
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

    private void enforceAvatarAccess(HttpServletRequest request, String objectName) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean authenticated = auth != null && auth.isAuthenticated()
                && !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken);
        if (authenticated) return;
        String shareToken = request.getParameter("shareToken");
        if (StringUtils.isNotBlank(shareToken)) {
            ResumeShare share = resumeShareMapper.selectOne(
                    new LambdaQueryWrapper<ResumeShare>().eq(ResumeShare::getToken, shareToken.trim()));
            if (share != null && "active".equals(share.getStatus()) && Integer.valueOf(0).equals(share.getDeleted())
                    && (share.getExpiresAt() == null || share.getExpiresAt().isAfter(LocalDateTime.now()))) {
                // 归属绑定：头像对象名为 {userId}/avatars/...，仅允许同用户分享 token 访问
                if (StringUtils.isNotBlank(share.getUserId())
                        && objectName.startsWith(share.getUserId() + "/")) {
                    return;
                }
                log.warn("ShareToken user mismatch for avatar access");
                throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "资源不存在。");
            }
            log.warn("Invalid shareToken for avatar access");
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "分享不存在或已过期。");
        }
        // 匿名无凭证统一 404，避免 401/404 预言机；Referer 可伪造，不再作为旁路
        throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "资源不存在。");
    }

    private void validateObjectName(String objectName) {
        if (StringUtils.isBlank(objectName) || objectName.length() > 512) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "文件路径不合法。");
        }
        // 原文含 % 即可能是编码穿越（%2e/%2f/%252e），直接拒绝，避免单次解码绕过
        String lowered = objectName.toLowerCase();
        if (lowered.contains("%")) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "文件路径不合法。");
        }
        String decoded = objectName;
        try {
            // 循环解码至稳定（上限2次），防 %252e 双重编码绕过
            for (int i = 0; i < 2; i++) {
                String next = java.net.URLDecoder.decode(decoded, java.nio.charset.StandardCharsets.UTF_8);
                if (next.equals(decoded)) break;
                decoded = next;
            }
        } catch (Exception e) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "文件路径不合法。");
        }
        if (decoded.contains("..") || decoded.contains("\\") || decoded.contains("//") || decoded.startsWith("/")) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "文件路径不合法。");
        }
        // 拒绝控制字符与空字节
        for (int i = 0; i < decoded.length(); i++) {
            char c = decoded.charAt(i);
            if (c <= 0x1F || c == 0x7F) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "文件路径不合法。");
            }
        }
        java.nio.file.Path normalized = java.nio.file.Paths.get(decoded).normalize();
        String normStr = normalized.toString().replace("\\", "/");
        if (normStr.contains("..") || normStr.startsWith("/") || normStr.startsWith("\\")) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "文件路径不合法。");
        }
    }

    private ResponseEntity<byte[]> buildResponse(String objectName, byte[] data, boolean cacheable) {
        return buildResponse(objectName, data, cacheable, guessContentType(objectName));
    }

    private ResponseEntity<byte[]> buildResponse(String objectName, byte[] data, boolean cacheable, String contentType) {
        org.springframework.http.ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'none'; img-src 'self'; style-src 'unsafe-inline'")
                .cacheControl(cacheable
                        ? CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic()
                        : CacheControl.noStore());
        return builder.body(data);
    }

    private String guessContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif")) return "image/gif";
        // HTML/JS/SVG 等可执行内容一律按二进制返回，防止同源存储型 XSS
        return "application/octet-stream";
    }
}
