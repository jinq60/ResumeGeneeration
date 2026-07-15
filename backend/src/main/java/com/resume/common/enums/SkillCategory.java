package com.resume.common.enums;

/**
 * 技能分类枚举。
 */
public enum SkillCategory {
    PROGRAMMING_LANGUAGE("programming_language", "编程语言"),
    FRONTEND("frontend", "前端"),
    BACKEND("backend", "后端"),
    DATABASE("database", "数据库"),
    AI_DATA("ai_data", "AI/数据"),
    DESIGN("design", "设计"),
    OFFICE("office", "办公软件"),
    LANGUAGE("language", "语言"),
    PROFESSIONAL_TOOL("professional_tool", "专业工具"),
    OTHER("other", "其他");

    private final String code;
    private final String description;

    SkillCategory(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static SkillCategory fromCode(String code) {
        for (SkillCategory category : values()) {
            if (category.code.equals(code)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Invalid SkillCategory code: " + code);
    }
}
