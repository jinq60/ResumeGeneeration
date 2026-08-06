package com.resume.resume.share.controller;

import com.resume.common.constant.ResultCode;
import com.resume.common.entity.R;
import com.resume.common.exception.BusinessException;
import com.resume.resume.share.dto.CreateShareRequest;
import com.resume.resume.share.dto.ShareResponse;
import com.resume.resume.share.service.ShareService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 简历分享接口。
 */
@RestController
@RequiredArgsConstructor
public class ShareController {

    private final ShareService shareService;

    /**
     * 创建/轮换分享。
     */
    @PostMapping("/resumes/{resumeId}/share")
    public R<ShareResponse> create(@AuthenticationPrincipal String userId,
                                   @PathVariable String resumeId,
                                   @Valid @RequestBody(required = false) CreateShareRequest request) {
        boolean hideContact = request != null && Boolean.TRUE.equals(request.getHideContact());
        return R.success(shareService.createShare(userId, resumeId, hideContact,
                request == null ? null : request.getExpiresAt()));
    }

    /**
     * 查询当前分享状态（无分享返回 data=null）。
     */
    @GetMapping("/resumes/{resumeId}/share")
    public R<ShareResponse> get(@AuthenticationPrincipal String userId,
                                @PathVariable String resumeId) {
        return R.success(shareService.getShare(userId, resumeId));
    }

    /**
     * 关闭分享。
     */
    @DeleteMapping("/resumes/{resumeId}/share")
    public R<Void> revoke(@AuthenticationPrincipal String userId,
                          @PathVariable String resumeId) {
        shareService.revokeShare(userId, resumeId);
        return R.success();
    }

    /**
     * 公开只读分享页（text/html）。
     */
    @GetMapping("/share/{token}")
    public void renderSharePage(@PathVariable String token, HttpServletResponse response) throws IOException {
        String html = shareService.renderSharePage(token);
        response.setContentType(MediaType.TEXT_HTML_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // 分享页含个人信息：禁止缓存；禁脚本与外部资源，防注入
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Content-Security-Policy",
                "default-src 'none'; img-src 'self' data:; style-src 'unsafe-inline'; frame-ancestors 'none'");
        response.getWriter().write(html);
    }
}
