package com.resume.common.constant;

/**
 * 响应状态码常量。
 */
public final class ResultCode {

    private ResultCode() {
    }

    public static final int SUCCESS = 200;
    public static final int PARAM_INVALID = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int ACCESS_DENIED = 403;
    public static final int RESOURCE_NOT_FOUND = 404;
    public static final int RATE_LIMITED = 429;
    public static final int INTERNAL_ERROR = 500;

    /**
     * 认证模块错误码：1000-1099
     */
    public static final int AUTH_PHONE_REGISTERED = 1000;
    public static final int AUTH_EMAIL_REGISTERED = 1001;
    public static final int AUTH_VERIFY_CODE_INVALID = 1002;
    public static final int AUTH_PASSWORD_TOO_WEAK = 1003;
    public static final int AUTH_ACCOUNT_NOT_FOUND = 1004;
    public static final int AUTH_PASSWORD_INCORRECT = 1005;
    public static final int AUTH_ACCOUNT_LOCKED = 1006;
    public static final int AUTH_REFRESH_TOKEN_INVALID = 1007;

    /**
     * 简历模块错误码：2000-2099
     */
    public static final int RESUME_SCENE_INVALID = 2000;
    public static final int RESUME_TEMPLATE_NOT_FOUND = 2001;
    public static final int RESUME_NOT_FOUND = 2002;
    public static final int RESUME_SECTION_INVALID = 2003;
    public static final int RESUME_PROFILE_NAME_REQUIRED = 2004;
    public static final int RESUME_PROFILE_CONTACT_REQUIRED = 2005;
    public static final int RESUME_CONTENT_TOO_SHORT = 2006;
    public static final int RESUME_PROFILE_PHONE_INVALID = 2007;
    public static final int RESUME_PROFILE_EMAIL_INVALID = 2008;
    public static final int RESUME_PROFILE_URL_INVALID = 2009;
    public static final int RESUME_CONTENT_TOO_LONG = 2010;

    /**
     * 模板模块错误码：3000-3099
     */
    public static final int TEMPLATE_NOT_FOUND = 3000;
    public static final int TEMPLATE_CODE_EXISTS = 3001;
    public static final int TEMPLATE_CONFIG_INVALID = 3002;
    public static final int TEMPLATE_CODE_IMMUTABLE = 3003;
    public static final int TEMPLATE_BUILTIN_PROTECTED = 3004;

    /**
     * 头像模块错误码：4000-4099
     */
    public static final int AVATAR_FILE_EMPTY = 4000;
    public static final int AVATAR_FORMAT_UNSUPPORTED = 4001;
    public static final int AVATAR_FILE_TOO_LARGE = 4002;
    public static final int AVATAR_SOURCE_NOT_FOUND = 4003;
    public static final int AVATAR_BACKGROUND_TYPE_INVALID = 4004;
    public static final int AVATAR_STYLE_INVALID = 4005;
    public static final int AVATAR_TASK_NOT_FOUND = 4006;
    public static final int AVATAR_OPTIMIZE_FAILED = 4007;
    public static final int AVATAR_LOAD_FAILED = 4011;

    /**
     * PDF 模块错误码：5000-5099
     */
    public static final int PDF_TASK_NOT_FOUND = 5000;
    public static final int PDF_FILE_NOT_READY = 5001;
    public static final int PDF_EXPORT_NAME_REQUIRED = 5002;
    public static final int PDF_EXPORT_CONTACT_REQUIRED = 5003;
    public static final int PDF_EXPORT_FAILED = 5004;

    /**
     * AI 模块错误码：6000-6099
     */
    public static final int AI_TASK_NOT_FOUND = 6000;
    public static final int AI_TASK_FAILED = 6001;
    public static final int AI_PROVIDER_NOT_CONFIGURED = 6002;
    public static final int AI_MODEL_CALL_FAILED = 6003;
    public static final int AI_CONCURRENT_LIMIT_EXCEEDED = 6004;
    public static final int AI_RESPONSE_PARSE_FAILED = 6005;
    public static final int AI_CONTENT_TOO_LONG = 6006;
}
