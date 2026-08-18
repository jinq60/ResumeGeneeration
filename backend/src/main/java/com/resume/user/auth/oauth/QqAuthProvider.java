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
 * QQ 互联 OAuth2 适配器（扫码登录）。
 */
@Slf4j
@Component
public class QqAuthProvider extends BaseOAuthProvider {

    private final AuthProperties.QqConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public QqAuthProvider(WebClient.Builder webClientBuilder, AuthProperties authProperties) {
        super(webClientBuilder, authProperties.getOauth());
        this.config = authProperties.getOauth().getQq();
    }

    @Override
    public String provider() {
        return AuthMethod.QQ;
    }

    @Override
    public boolean isConfigured() {
        return StringUtils.isNotBlank(config.getAppId())
                && StringUtils.isNotBlank(config.getAppKey());
    }

    @Override
    public String buildAuthorizeUrl(String state) {
        return config.getAuthorizeUrl() + "?response_type=code&client_id=" + encode(config.getAppId())
                + "&redirect_uri=" + encode(redirectUri())
                + "&state=" + encode(state) + "&scope=get_user_info";
    }

    @Override
    public OAuthUserInfo exchangeAndFetch(String code) {
        String accessToken = exchangeToken(config.getTokenUrl(), config.getAppId(),
                config.getAppKey(), redirectUri(), code);
        String openId = fetchOpenId(accessToken);
        JsonNode userInfo = fetchUserInfo(accessToken, openId);
        return new OAuthUserInfo(AuthMethod.QQ, openId, null,
                userInfo.path("nickname").asText(null),
                userInfo.path("figureurl_qq_2").asText(null),
                false);
    }

    /**
     * QQ OpenID 接口返回 callback({...}) 包裹的 JSON，需剥离。
     */
    private String fetchOpenId(String accessToken) {
        try {
            String raw = webClient.get()
                    .uri(config.getOpenIdUrl() + "?access_token=" + encode(accessToken))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(HTTP_TIMEOUT)
                    .block();
            if (raw == null) {
                throw new IllegalStateException("empty openid response");
            }
            int start = raw.indexOf('{');
            int end = raw.lastIndexOf('}');
            if (start < 0 || end <= start) {
                throw new IllegalStateException("invalid openid response");
            }
            JsonNode node = objectMapper.readTree(raw.substring(start, end + 1));
            String openId = node.path("openid").asText(null);
            if (StringUtils.isBlank(openId)) {
                throw new IllegalStateException("missing openid");
            }
            return openId;
        } catch (Exception e) {
            log.warn("Fetch QQ openid failed: {}", e.getMessage());
            throw new com.resume.common.exception.BusinessException(
                    com.resume.common.constant.ResultCode.AUTH_OAUTH_EXCHANGE_FAILED,
                    "QQ 登录失败，请稍后重试。");
        }
    }

    private JsonNode fetchUserInfo(String accessToken, String openId) {
        try {
            String json = webClient.get()
                    .uri(config.getUserInfoUrl() + "?access_token=" + encode(accessToken)
                            + "&oauth_consumer_key=" + encode(config.getAppId())
                            + "&openid=" + encode(openId))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(HTTP_TIMEOUT)
                    .block();
            JsonNode node = objectMapper.readTree(json);
            if (node.path("ret").asInt(-1) != 0) {
                throw new IllegalStateException("qq userinfo ret=" + node.path("ret").asText());
            }
            return node;
        } catch (Exception e) {
            log.warn("Fetch QQ user info failed: {}", e.getMessage());
            throw new com.resume.common.exception.BusinessException(
                    com.resume.common.constant.ResultCode.AUTH_OAUTH_EXCHANGE_FAILED,
                    "QQ 登录失败，请稍后重试。");
        }
    }

    private String redirectUri() {
        return redirectUri("/auth/oauth/qq/callback");
    }
}
