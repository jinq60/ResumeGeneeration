package com.resume.common.enums;

/**
 * 工作类型枚举。
 */
public enum WorkType {
    FULL_TIME("full_time", "全职"),
    INTERNSHIP("internship", "实习"),
    PART_TIME("part_time", "兼职"),
    CAMPUS_JOB("campus_job", "校园兼职"),
    RESEARCH_ASSISTANT("research_assistant", "研究助理"),
    VOLUNTEER("volunteer", "志愿者");

    private final String code;
    private final String description;

    WorkType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static WorkType fromCode(String code) {
        for (WorkType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid WorkType code: " + code);
    }
}
