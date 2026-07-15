package com.resume.common.enums;

/**
 * 技能熟练程度枚举。
 */
public enum SkillLevel {
    BEGINNER("beginner", "入门"),
    FAMILIAR("familiar", "熟悉"),
    PROFICIENT("proficient", "熟练"),
    EXPERT("expert", "精通");

    private final String code;
    private final String description;

    SkillLevel(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static SkillLevel fromCode(String code) {
        for (SkillLevel level : values()) {
            if (level.code.equals(code)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Invalid SkillLevel code: " + code);
    }
}
