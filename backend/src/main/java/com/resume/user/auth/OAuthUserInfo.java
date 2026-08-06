package com.resume.user.auth;

/**
 * OAuth 用户信息（登录成功后的第三方资料）。
 */
public record OAuthUserInfo(String provider, String providerAccountId,
                            String email, String nickname, String avatarUrl) {
}
