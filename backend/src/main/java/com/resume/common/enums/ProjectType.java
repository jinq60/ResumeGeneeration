package com.resume.common.enums;

/**
 * 项目类型枚举。
 */
public enum ProjectType {
    RESEARCH("research", "研究项目"),
    COURSE("course", "课程项目"),
    ENTERPRISE("enterprise", "企业项目"),
    COMPETITION("competition", "竞赛项目"),
    OPEN_SOURCE("open_source", "开源项目"),
    PERSONAL("personal", "个人项目"),
    OTHER("other", "其他");

    private final String code;
    private final String description;

    ProjectType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ProjectType fromCode(String code) {
        for (ProjectType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid ProjectType code: " + code);
    }
}
