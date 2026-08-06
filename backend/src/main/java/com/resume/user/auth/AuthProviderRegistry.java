package com.resume.user.auth;

import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 认证适配器注册表：自动收集所有 {@link AuthProvider} 与 {@link OAuthProvider}。
 */
@Slf4j
@Component
public class AuthProviderRegistry {

    private final Map<String, AuthProvider> loginProviders = new HashMap<>();
    private final Map<String, OAuthProvider> oauthProviders = new HashMap<>();

    public AuthProviderRegistry(ApplicationContext applicationContext) {
        for (AuthProvider provider : applicationContext.getBeansOfType(AuthProvider.class).values()) {
            loginProviders.put(provider.method(), provider);
            log.info("Registered auth provider: {}", provider.method());
        }
        for (OAuthProvider provider : applicationContext.getBeansOfType(OAuthProvider.class).values()) {
            oauthProviders.put(provider.provider(), provider);
            log.info("Registered oauth provider: {}", provider.provider());
        }
    }

    /**
     * 登录适配器列表（含未配置项，供前端渲染入口）。
     */
    public List<AuthProvider> allLoginProviders() {
        return List.copyOf(loginProviders.values());
    }

    /**
     * OAuth 适配器列表（含未配置项，供前端渲染入口）。
     */
    public List<OAuthProvider> allOAuthProviders() {
        return List.copyOf(oauthProviders.values());
    }

    public AuthProvider getLoginProvider(String method) {
        AuthProvider provider = loginProviders.get(method);
        if (provider == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "不支持的登录方式：" + method);
        }
        return provider;
    }

    public OAuthProvider getOAuthProvider(String provider) {
        OAuthProvider oauth = oauthProviders.get(provider);
        if (oauth == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "不支持的第三方登录：" + provider);
        }
        return oauth;
    }
}
