package com.resume.user.auth.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.user.auth.AuthMethod;
import com.resume.user.auth.OAuthUserInfo;
import com.resume.user.config.AuthProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Google OAuth2 适配器（OpenID Connect）。
 */
@Slf4j
@Component
public class GoogleAuthProvider extends BaseOAuthProvider {

    private final AuthProperties.GoogleConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GoogleAuthProvider(WebClient.Builder webClientBuilder, AuthProperties authProperties) {
        super(webClientBuilder, authProperties.getOauth());
        this.config = authProperties.getOauth().getGoogle();
    }

    @Override
    public String provider() {
        return AuthMethod.GOOGLE;
    }

    @Override
    public boolean isConfigured() {
        return StringUtils.isNotBlank(config.getClientId())
                && StringUtils.isNotBlank(config.getClientSecret());
    }

    @Override
    public String buildAuthorizeUrl(String state) {
        return config.getAuthorizeUrl() + "?client_id=" + encode(config.getClientId())
                + "&redirect_uri=" + encode(redirectUri())
                + "&response_type=code&scope=" + encode("openid email profile")
                + "&state=" + encode(state) + "&prompt=select_account";
    }

    @Override
    public OAuthUserInfo exchangeAndFetch(String code) {
        String accessToken = exchangeToken(config.getTokenUrl(), config.getClientId(),
                config.getClientSecret(), redirectUri(), code);
        String json = fetchJson(config.getUserInfoUrl(), accessToken, "application/json");
        try {
            JsonNode node = objectMapper.readTree(json);
            String sub = node.path("sub").asText();
            if (StringUtils.isBlank(sub)) {
                throw new IllegalStateException("missing sub");
            }
            return new OAuthUserInfo(AuthMethod.GOOGLE, sub,
                    node.path("email").asText(null),
                    node.path("name").asText(null),
                    node.path("picture").asText(null),
                    node.path("email_verified").asBoolean(false));
        } catch (Exception e) {
            log.warn("Parse Google user info failed: {}", e.getMessage());
            throw new com.resume.common.exception.BusinessException(
                    com.resume.common.constant.ResultCode.AUTH_OAUTH_EXCHANGE_FAILED,
                    "第三方登录失败：解析用户信息异常。");
        }
    }

    private String redirectUri() {
        return redirectUri("/auth/oauth/google/callback");
    }
}
