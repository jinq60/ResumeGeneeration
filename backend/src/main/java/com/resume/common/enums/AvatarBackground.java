package com.resume.common.enums;

/**
 * 头像背景类型枚举。
 */
public enum AvatarBackground {
    WHITE("white", "白色背景"),
    BLUE("blue", "蓝色背景"),
    RED("red", "红色背景"),
    TRANSPARENT("transparent", "透明背景");

    private final String code;
    private final String description;

    AvatarBackground(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static AvatarBackground fromCode(String code) {
        for (AvatarBackground background : values()) {
            if (background.code.equals(code)) {
                return background;
            }
        }
        throw new IllegalArgumentException("Invalid AvatarBackground code: " + code);
    }
}
