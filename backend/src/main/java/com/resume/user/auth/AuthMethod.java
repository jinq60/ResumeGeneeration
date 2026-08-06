package com.resume.user.auth;

/**
 * 认证方式标识。
 */
public final class AuthMethod {

    public static final String PASSWORD = "password";
    public static final String EMAIL_CODE = "email_code";
    public static final String SMS_CODE = "sms_code";
    public static final String GOOGLE = "google";
    public static final String GITHUB = "github";
    public static final String QQ = "qq";

    private AuthMethod() {
    }
}
