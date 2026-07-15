package com.resume.common.enums;

/**
 * 自我介绍风格枚举。
 */
public enum IntroductionStyle {
    CONCISE_FORMAL("concise_formal", "简洁正式"),
    TECH_ORIENTED("tech_oriented", "技术导向"),
    STUDENT("student", "学生风格"),
    SENIOR("senior", "资深风格"),
    POSTGRADUATE("postgraduate", "考研风格"),
    PROJECT("project", "项目风格");

    private final String code;
    private final String description;

    IntroductionStyle(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static IntroductionStyle fromCode(String code) {
        for (IntroductionStyle style : values()) {
            if (style.code.equals(code)) {
                return style;
            }
        }
        throw new IllegalArgumentException("Invalid IntroductionStyle code: " + code);
    }
}
