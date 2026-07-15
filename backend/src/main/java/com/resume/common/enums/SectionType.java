package com.resume.common.enums;

/**
 * Section 类型枚举。
 */
public enum SectionType {
    PROFILE("profile", "个人信息"),
    EDUCATION("education", "教育经历"),
    PROJECT("project", "项目经历"),
    WORK("work", "工作经历"),
    SKILL("skill", "技能"),
    INTRODUCTION("introduction", "自我介绍"),
    CUSTOM("custom", "自定义");

    private final String code;
    private final String description;

    SectionType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static SectionType fromCode(String code) {
        for (SectionType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid SectionType code: " + code);
    }
}
