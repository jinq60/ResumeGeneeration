package com.resume.user.controller;

import com.resume.common.entity.R;
import com.resume.user.dto.*;
import com.resume.user.service.UserService;
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

    @PostMapping("/register")
    public R<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return R.success(userService.register(request));
    }

    @PostMapping("/login")
    public R<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return R.success(userService.login(request));
    }

    @PostMapping("/guest")
    public R<AuthResponse> guest() {
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
}
