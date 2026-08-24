package com.resume.user.auth.provider;

import com.resume.user.auth.oauth.GoogleAuthProvider;
import com.resume.user.auth.oauth.GithubAuthProvider;
import com.resume.user.auth.oauth.QqAuthProvider;
import com.resume.user.config.AuthProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * 认证适配器配置状态测试：验证 isConfigured() 与配置项绑定正确。
 */
class AuthProviderConfigTest {

    private final WebClient.Builder webClientBuilder = WebClient.builder();

    @Test
    void emailCodeAuthProvider_shouldBeConfiguredWhenSmtpIsSet() {
        AuthProperties properties = new AuthProperties();
        properties.getSmtp().setHost("smtp.qq.com");
        properties.getSmtp().setPort(465);
        properties.getSmtp().setUsername("test@qq.com");
        properties.getSmtp().setPassword("auth-code");

        EmailCodeAuthProvider provider = new EmailCodeAuthProvider(mock(), properties);
        assertTrue(provider.isConfigured());
    }

    @Test
    void emailCodeAuthProvider_shouldNotBeConfiguredWhenSmtpIsMissing() {
        AuthProperties properties = new AuthProperties();
        // host/username/password 任一为空即视为未配置
        properties.getSmtp().setHost("smtp.qq.com");
        properties.getSmtp().setUsername("test@qq.com");

        EmailCodeAuthProvider provider = new EmailCodeAuthProvider(mock(), properties);
        assertFalse(provider.isConfigured());
    }

    @Test
    void githubAuthProvider_shouldBeConfiguredWhenClientCredentialsAreSet() {
        AuthProperties properties = new AuthProperties();
        properties.getOauth().getGithub().setClientId("client-id");
        properties.getOauth().getGithub().setClientSecret("client-secret");

        GithubAuthProvider provider = new GithubAuthProvider(webClientBuilder, properties);
        assertTrue(provider.isConfigured());
    }

    @Test
    void githubAuthProvider_shouldNotBeConfiguredWhenClientCredentialsAreMissing() {
        AuthProperties properties = new AuthProperties();
        GithubAuthProvider provider = new GithubAuthProvider(webClientBuilder, properties);
        assertFalse(provider.isConfigured());
    }

    @Test
    void googleAuthProvider_shouldBeConfiguredWhenClientCredentialsAreSet() {
        AuthProperties properties = new AuthProperties();
        properties.getOauth().getGoogle().setClientId("client-id");
        properties.getOauth().getGoogle().setClientSecret("client-secret");

        GoogleAuthProvider provider = new GoogleAuthProvider(webClientBuilder, properties);
        assertTrue(provider.isConfigured());
    }

    @Test
    void googleAuthProvider_shouldNotBeConfiguredWhenClientCredentialsAreMissing() {
        AuthProperties properties = new AuthProperties();
        GoogleAuthProvider provider = new GoogleAuthProvider(webClientBuilder, properties);
        assertFalse(provider.isConfigured());
    }

    @Test
    void qqAuthProvider_shouldBeConfiguredWhenAppCredentialsAreSet() {
        AuthProperties properties = new AuthProperties();
        properties.getOauth().getQq().setAppId("app-id");
        properties.getOauth().getQq().setAppKey("app-key");

        QqAuthProvider provider = new QqAuthProvider(webClientBuilder, properties);
        assertTrue(provider.isConfigured());
    }

    @Test
    void qqAuthProvider_shouldNotBeConfiguredWhenAppCredentialsAreMissing() {
        AuthProperties properties = new AuthProperties();
        QqAuthProvider provider = new QqAuthProvider(webClientBuilder, properties);
        assertFalse(provider.isConfigured());
    }
}
