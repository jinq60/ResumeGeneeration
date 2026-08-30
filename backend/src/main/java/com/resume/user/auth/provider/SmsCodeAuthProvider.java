package com.resume.user.auth.provider;

import com.resume.user.auth.AuthMethod;
import com.resume.user.auth.AuthProvider;
import com.resume.user.auth.SmsCodeService;
import com.resume.user.auth.UserAuthService;
import com.resume.user.dto.AuthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 手机短信验证码登录适配器（免密，首次登录自动创建账号）。
 */
@Component
@RequiredArgsConstructor
public class SmsCodeAuthProvider implements AuthProvider {

    private final UserAuthService userAuthService;
    private final SmsCodeService smsCodeService;

    @Override
    public String method() {
        return AuthMethod.SMS_CODE;
    }

    @Override
    public AuthResponse authenticate(Map<String, String> params) {
        return userAuthService.authenticateBySmsCode(params.get("phone"), params.get("code"));
    }

    @Override
    public boolean isConfigured() {
        return smsCodeService.isConfigured();
    }
}
