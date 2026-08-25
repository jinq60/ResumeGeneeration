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
            // 安全：/user 接口返回的 profile email 不代表已验证（攻击者可将未验证的
            // 受害者邮箱设为公开邮箱），一律忽略；仅接受 /user/emails 中
            // primary==true && verified==true 的已验证主邮箱，防止账号接管。
            String email = fetchPrimaryEmail(accessToken);
            return new OAuthUserInfo(AuthMethod.GITHUB, id, email,
                    node.path("name").asText(null),
                    node.path("avatar_url").asText(null),
                    StringUtils.isNotBlank(email));
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
            return extractVerifiedPrimaryEmail(objectMapper.readTree(emails));
        } catch (Exception e) {
            log.debug("Fetch GitHub primary email failed: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 从 /user/emails 响应中提取 primary==true 且 verified==true 的邮箱。
     * <p>
     * 安全约束：未验证或非主邮箱一律不采纳，找不到已验证主邮箱时返回 null
     * （调用方将得到 emailVerified=false，从而不会按邮箱关联既有账号）。
     * </p>
     */
    static String extractVerifiedPrimaryEmail(JsonNode array) {
        if (array == null || !array.isArray()) {
            return null;
        }
        for (JsonNode item : array) {
            boolean primary = item.path("primary").asBoolean(false);
            boolean verified = item.path("verified").asBoolean(false);
            if (primary && verified) {
                String email = item.path("email").asText(null);
                if (StringUtils.isNotBlank(email)) {
                    return email;
                }
            }
        }
        // 不做任何兜底：宁可不关联既有账号，也不能采信未验证邮箱
        return null;
    }

    private String redirectUri() {
        return redirectUri("/auth/oauth/github/callback");
    }
}
