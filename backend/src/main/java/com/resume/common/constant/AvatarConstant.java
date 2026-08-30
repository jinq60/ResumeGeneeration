package com.resume.common.constant;

/**
 * 头像域常量。
 */
public final class AvatarConstant {

    private AvatarConstant() {
    }

    public static final String AVATAR_BACKGROUND_WHITE = "white";
    public static final String AVATAR_BACKGROUND_BLUE = "blue";
    public static final String AVATAR_BACKGROUND_RED = "red";
    public static final String AVATAR_BACKGROUND_TRANSPARENT = "transparent";

    public static final java.util.Set<String> AVATAR_BACKGROUND_TYPES_P0 = java.util.Set.of(
            AVATAR_BACKGROUND_WHITE,
            AVATAR_BACKGROUND_BLUE,
            AVATAR_BACKGROUND_RED
    );

    public static final java.util.Set<String> AVATAR_BACKGROUND_TYPES_ALL = java.util.Set.of(
            AVATAR_BACKGROUND_WHITE,
            AVATAR_BACKGROUND_BLUE,
            AVATAR_BACKGROUND_RED,
            AVATAR_BACKGROUND_TRANSPARENT
    );

    public static final String AVATAR_STYLE_FORMAL = "formal";
    public static final String AVATAR_STYLE_NATURAL = "natural";
    public static final String AVATAR_STYLE_PROFESSIONAL = "professional";

    public static final java.util.Set<String> AVATAR_STYLES = java.util.Set.of(
            AVATAR_STYLE_FORMAL,
            AVATAR_STYLE_NATURAL,
            AVATAR_STYLE_PROFESSIONAL
    );
}
