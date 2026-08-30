package com.resume.user.auth;

import com.resume.user.dto.AuthResponse;

import java.util.Map;

/**
 * 登录认证适配器（账号密码 / 验证码等免密方式）。
 * <p>
 * 每种登录方式一个实现，通过 {@code method()} 注册；
 * 由 {@link AuthProviderRegistry} 统一收集与分发。
 * </p>
 */
public interface AuthProvider {

    /**
     * 认证方式标识（见 {@link AuthMethod}）。
     */
    String method();

    /**
     * 执行认证并返回 JWT 会话。
     *
     * @param params 认证所需参数（如 account/password/email/code）
     */
    AuthResponse authenticate(Map<String, String> params);

    /**
     * 执行认证（带客户端 IP，用于 IP 维度限流/喷洒防御）。
     * 默认委托不带 IP 的实现，Password 等需 IP 维度的提供商应重写。
     */
    default AuthResponse authenticate(Map<String, String> params, String clientIp) {
        return authenticate(params);
    }

    /**
     * 是否已配置可用（未配置时返回降级结果）。
     */
    default boolean isConfigured() {
        return true;
    }
}
