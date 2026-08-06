package com.resume.user.auth.provider;

import com.resume.user.auth.AuthMethod;
import com.resume.user.auth.AuthProvider;
import com.resume.user.auth.UserAuthService;
import com.resume.user.dto.AuthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 邮箱验证码登录适配器（免密，首次登录自动创建账号）。
 */
@Component
@RequiredArgsConstructor
public class EmailCodeAuthProvider implements AuthProvider {

    private final UserAuthService userAuthService;

    @Override
    public String method() {
        return AuthMethod.EMAIL_CODE;
    }

    @Override
    public AuthResponse authenticate(Map<String, String> params) {
        return userAuthService.authenticateByEmailCode(params.get("email"), params.get("code"));
    }
}
