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
 * GitHub OAuth2 适配器。
 */
@Slf4j
@Component
public class GithubAuthProvider extends BaseOAuthProvider {

    private final AuthProperties.GithubConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GithubAuthProvider(WebClient.Builder webClientBuilder, AuthProperties authProperties) {
        super(webClientBuilder, authProperties.getOauth());
        this.config = authProperties.getOauth().getGithub();
    }

    @Override
    public String provider() {
        return AuthMethod.GITHUB;
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
                + "&scope=" + encode("read:user user:email")
                + "&state=" + encode(state);
    }

    @Override
    public OAuthUserInfo exchangeAndFetch(String code) {
        String accessToken = exchangeToken(config.getTokenUrl(), config.getClientId(),
                config.getClientSecret(), redirectUri(), code);
        String json = fetchJson(config.getUserInfoUrl(), accessToken, "application/vnd.github+json");
        try {
            JsonNode node = objectMapper.readTree(json);
            String id = node.path("id").asText();
            if (StringUtils.isBlank(id)) {
                throw new IllegalStateException("missing id");
            }
            String email = node.path("email").asText(null);
            if (StringUtils.isBlank(email)) {
                email = fetchPrimaryEmail(accessToken);
            }
            return new OAuthUserInfo(AuthMethod.GITHUB, id, email,
                    node.path("name").asText(null),
                    node.path("avatar_url").asText(null));
        } catch (Exception e) {
            log.warn("Parse GitHub user info failed: {}", e.getMessage());
            throw new com.resume.common.exception.BusinessException(
                    com.resume.common.constant.ResultCode.AUTH_OAUTH_EXCHANGE_FAILED,
                    "第三方登录失败：解析用户信息异常。");
        }
    }

    private String fetchPrimaryEmail(String accessToken) {
        try {
            String emails = fetchJson(config.getEmailsUrl(), accessToken, "application/vnd.github+json");
            JsonNode array = objectMapper.readTree(emails);
            for (JsonNode item : array) {
                if (item.path("primary").asBoolean(false)) {
                    return item.path("email").asText(null);
                }
            }
            if (array.isArray() && array.size() > 0) {
                return array.get(0).path("email").asText(null);
            }
        } catch (Exception e) {
            log.debug("Fetch GitHub primary email failed: {}", e.getMessage());
        }
        return null;
    }

    private String redirectUri() {
        return redirectUri("/auth/oauth/github/callback");
    }
}
