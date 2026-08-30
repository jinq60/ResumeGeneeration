package com.resume.common.constant;

/**
 * 用户域常量。
 * <p>
 * 从 {@link BizConstant} 拆分而来，后续新增用户相关常量请置于此处；
 * {@link BizConstant} 保留兼容代理。
 * </p>
 */
public final class UserConstant {

    private UserConstant() {
    }

    public static final String USER_STATUS_ACTIVE = "active";
    public static final String USER_STATUS_DISABLED = "disabled";

    public static final String USER_ROLE_USER = "USER";
    public static final String USER_ROLE_ADMIN = "ADMIN";

    public static final Integer IS_GUEST = 1;
    public static final Integer IS_NOT_GUEST = 0;

    public static final String GENDER_MALE = "male";
    public static final String GENDER_FEMALE = "female";
    public static final String GENDER_OTHER = "other";

    public static final java.util.Set<String> GENDERS = java.util.Set.of(
            GENDER_MALE,
            GENDER_FEMALE,
            GENDER_OTHER
    );
}
