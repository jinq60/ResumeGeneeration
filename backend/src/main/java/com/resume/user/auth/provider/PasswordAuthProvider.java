package com.resume.user.auth.provider;

import com.resume.common.exception.BusinessException;
import com.resume.user.auth.AuthMethod;
import com.resume.user.auth.AuthProvider;
import com.resume.user.dto.AuthResponse;
import com.resume.user.dto.LoginRequest;
import com.resume.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 账号密码登录适配器（复用现有用户名密码登录链路）。
 */
@Component
@RequiredArgsConstructor
public class PasswordAuthProvider implements AuthProvider {

    private final UserService userService;

    @Override
    public String method() {
        return AuthMethod.PASSWORD;
    }

    @Override
    public AuthResponse authenticate(Map<String, String> params) {
        return authenticate(params, null);
    }

    @Override
    public AuthResponse authenticate(Map<String, String> params, String clientIp) {
        LoginRequest request = new LoginRequest();
        request.setAccount(params.get("account"));
        request.setPassword(params.get("password"));
        request.setLoginType(params.get("loginType"));
        if (request.getAccount() == null || request.getPassword() == null) {
            throw new BusinessException(com.resume.common.constant.ResultCode.PARAM_INVALID,
                    "请填写账号和密码。");
        }
        // 保留空白校验由 @Valid 完成；此处补 isBlank 防止绕过 Bean Validation 的手工 Map 入参
        if (request.getAccount().isBlank() || request.getPassword().isBlank()) {
            throw new BusinessException(com.resume.common.constant.ResultCode.PARAM_INVALID,
                    "请填写账号和密码。");
        }
        return userService.login(request, clientIp);
    }
}
