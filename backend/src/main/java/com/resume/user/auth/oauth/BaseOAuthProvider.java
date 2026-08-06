package com.resume.user.auth.oauth;

import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.user.auth.OAuthProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * OAuth 适配器公共实现：授权码换 token → 拉取用户信息。
 */
@Slf4j
public abstract class BaseOAuthProvider implements OAuthProvider {

    protected static final Duration HTTP_TIMEOUT = Duration.ofSeconds(15);

    protected final WebClient webClient;
    protected final com.resume.user.config.AuthProperties.OAuthConfig oauthConfig;

    protected BaseOAuthProvider(WebClient.Builder webClientBuilder,
                                com.resume.user.config.AuthProperties.OAuthConfig oauthConfig) {
        this.webClient = webClientBuilder
                .codecs(configurer -> configurer.defaultCodecs()
                        .maxInMemorySize(2 * 1024 * 1024))
                .build();
        this.oauthConfig = oauthConfig;
    }

    /**
     * 回调地址（后端绝对地址）。
     */
    protected String redirectUri(String path) {
        return oauthConfig.getBaseUrl() + path;
    }

    /**
     * 授权码换取 access token（表单 POST）。
     */
    protected String exchangeToken(String tokenUrl, String clientId, String clientSecret,
                                   String redirectUri, String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", redirectUri);
        form.add("code", code);
        try {
            String body = webClient.post()
                    .uri(tokenUrl)
                    .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(form))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(HTTP_TIMEOUT)
                    .block();
            return parseToken(body);
        } catch (Exception e) {
            log.warn("OAuth token exchange failed: {}", e.getMessage());
            throw new BusinessException(ResultCode.AUTH_OAUTH_EXCHANGE_FAILED,
                    "第三方登录失败，请稍后重试。");
        }
    }

    /**
     * GET 拉取用户信息（Bearer token）。
     */
    protected String fetchJson(String url, String accessToken, String accept) {
        try {
            return webClient.get()
                    .uri(url)
                    .headers(headers -> {
                        headers.setBearerAuth(accessToken);
                        if (accept != null) {
                            headers.set("Accept", accept);
                        }
                    })
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(HTTP_TIMEOUT)
                    .block();
        } catch (Exception e) {
            log.warn("OAuth user info fetch failed: url={}, error={}", url, e.getMessage());
            throw new BusinessException(ResultCode.AUTH_OAUTH_EXCHANGE_FAILED,
                    "第三方登录失败，请稍后重试。");
        }
    }

    protected static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String parseToken(String body) {
        if (body == null) {
            throw new BusinessException(ResultCode.AUTH_OAUTH_EXCHANGE_FAILED, "第三方登录失败：无响应。");
        }
        // JSON 响应：{"access_token":"..."}
        if (body.trim().startsWith("{")) {
            int idx = body.indexOf("\"access_token\":\"");
            if (idx >= 0) {
                int start = idx + "\"access_token\":\"".length();
                int end = body.indexOf('"', start);
                if (end > start) {
                    return body.substring(start, end);
                }
            }
            throw new BusinessException(ResultCode.AUTH_OAUTH_EXCHANGE_FAILED,
                    "第三方登录失败：未获取到授权令牌。");
        }
        // 查询串响应：access_token=xxx&expires_in=...
        for (String pair : body.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && "access_token".equals(pair.substring(0, eq))) {
                return pair.substring(eq + 1);
            }
        }
        throw new BusinessException(ResultCode.AUTH_OAUTH_EXCHANGE_FAILED,
                "第三方登录失败：未获取到授权令牌。");
    }
}
