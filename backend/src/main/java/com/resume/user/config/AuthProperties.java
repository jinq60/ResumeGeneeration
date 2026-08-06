package com.resume.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 多方式认证配置（验证码、第三方 OAuth、SMTP）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    private EmailCodeConfig emailCode = new EmailCodeConfig();
    private OAuthConfig oauth = new OAuthConfig();
    private SmtpConfig smtp = new SmtpConfig();

    @Data
    public static class EmailCodeConfig {
        private int ttlMinutes = 5;
        private int resendIntervalSeconds = 60;
    }

    @Data
    public static class OAuthConfig {
        /**
         * 后端对外基础地址（回调地址 = baseUrl + /auth/oauth/{provider}/callback）。
         */
        private String baseUrl = "http://localhost:8080/api";
        /**
         * 登录成功后前端跳转地址（携带 token query）。
         */
        private String frontendRedirect = "http://localhost:5173/login";
        private GoogleConfig google = new GoogleConfig();
        private GithubConfig github = new GithubConfig();
        private QqConfig qq = new QqConfig();
    }

    @Data
    public static class GoogleConfig {
        private String clientId;
        private String clientSecret;
        private String authorizeUrl = "https://accounts.google.com/o/oauth2/v2/auth";
        private String tokenUrl = "https://oauth2.googleapis.com/token";
        private String userInfoUrl = "https://openidconnect.googleapis.com/v1/userinfo";
    }

    @Data
    public static class GithubConfig {
        private String clientId;
        private String clientSecret;
        private String authorizeUrl = "https://github.com/login/oauth/authorize";
        private String tokenUrl = "https://github.com/login/oauth/access_token";
        private String userInfoUrl = "https://api.github.com/user";
        private String emailsUrl = "https://api.github.com/user/emails";
    }

    @Data
    public static class QqConfig {
        private String appId;
        private String appKey;
        private String authorizeUrl = "https://graph.qq.com/oauth2.0/authorize";
        private String tokenUrl = "https://graph.qq.com/oauth2.0/token";
        private String openIdUrl = "https://graph.qq.com/oauth2.0/me";
        private String userInfoUrl = "https://graph.qq.com/user/get_user_info";
    }

    @Data
    public static class SmtpConfig {
        private String host;
        private Integer port = 587;
        private String username;
        private String password;
        private String from;
    }
}
