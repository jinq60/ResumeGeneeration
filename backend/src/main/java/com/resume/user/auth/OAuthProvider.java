package com.resume.user.auth;

/**
 * 第三方 OAuth 适配器（Google / GitHub / QQ）。
 */
public interface OAuthProvider {

    /**
     * 提供商标识（google / github / qq）。
     */
    String provider();

    /**
     * 是否已配置 clientId/secret（未配置时前端按钮不可用）。
     */
    boolean isConfigured();

    /**
     * 生成第三方授权页跳转 URL。
     *
     * @param state 防 CSRF 随机串
     */
    String buildAuthorizeUrl(String state);

    /**
     * 用授权码换取用户信息。
     */
    OAuthUserInfo exchangeAndFetch(String code);
}
