package com.resume.user.controller;

import com.resume.common.constant.ResultCode;
import com.resume.common.entity.R;
import com.resume.common.exception.BusinessException;
import com.resume.user.dto.*;
import com.resume.user.service.GuestAccountGuard;
import com.resume.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 认证相关接口。
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final GuestAccountGuard guestAccountGuard;

    @PostMapping("/register")
    public R<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return R.success(userService.register(request));
    }

    @PostMapping("/login")
    public R<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return R.success(userService.login(request));
    }

    @PostMapping("/guest")
    public R<AuthResponse> guest(HttpServletRequest servletRequest) {
        // 每 IP 每日游客会话限额，防止刷号
        if (!guestAccountGuard.tryAcquire(resolveClientIp(servletRequest))) {
            throw new BusinessException(ResultCode.RATE_LIMITED, "游客体验名额已用完，请注册账号后使用。");
        }
        return R.success(userService.createGuest());
    }

    @PostMapping("/refresh")
    public R<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return R.success(userService.refresh(request));
    }

    @PostMapping("/logout")
    public R<Void> logout(@Valid @RequestBody LogoutRequest request) {
        userService.logout(request);
        return R.success();
    }

    /**
     * 获取客户端 IP：不信任 X-Forwarded-For，防止伪造绕过限额（与 RateLimitFilter 口径一致）。
     */
    private String resolveClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
