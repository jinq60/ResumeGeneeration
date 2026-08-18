package com.resume.user.auth;

/**
 * OAuth 用户信息（登录成功后的第三方资料）。
 *
 * @param emailVerified 邮箱是否已经第三方验证；仅已验证的邮箱才可用于关联既有账号，防止账号接管
 */
public record OAuthUserInfo(String provider, String providerAccountId,
                            String email, String nickname, String avatarUrl,
                            boolean emailVerified) {
}
