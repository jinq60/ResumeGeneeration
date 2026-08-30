package com.resume.common.constant;

/**
 * 简历域常量。
 * <p>
 * 从 {@link BizConstant} 拆分而来，后续新增简历相关常量请置于此处；
 * {@link BizConstant} 保留兼容代理。枚举真理源见
 * {@link com.resume.common.enums.SectionType} 等。
 * </p>
 */
public final class ResumeConstant {

    private ResumeConstant() {
    }

    public static final String RESUME_STATUS_ACTIVE = "active";
    public static final String RESUME_STATUS_DELETED = "deleted";

    public static final String SCENE_CAMPUS_RECRUITMENT = "campus_recruitment";
    public static final String SCENE_INTERNSHIP = "internship";
    public static final String SCENE_SOCIAL_RECRUITMENT = "social_recruitment";
    public static final String SCENE_POSTGRADUATE_REEXAM = "postgraduate_reexam";
    public static final String SCENE_PROJECT_APPLICATION = "project_application";
    public static final String SCENE_CUSTOM = "custom";

    public static final java.util.Set<String> SCENES = java.util.Set.of(
            SCENE_CAMPUS_RECRUITMENT,
            SCENE_INTERNSHIP,
            SCENE_SOCIAL_RECRUITMENT,
            SCENE_POSTGRADUATE_REEXAM,
            SCENE_PROJECT_APPLICATION,
            SCENE_CUSTOM
    );

    public static final String SECTION_TYPE_PROFILE = "profile";
    public static final String SECTION_TYPE_EDUCATION = "education";
    public static final String SECTION_TYPE_PROJECT = "project";
    public static final String SECTION_TYPE_WORK = "work";
    public static final String SECTION_TYPE_SKILL = "skill";
    public static final String SECTION_TYPE_INTRODUCTION = "introduction";
    public static final String SECTION_TYPE_CUSTOM = "custom";

    public static final java.util.Set<String> SECTION_TYPES = java.util.Set.of(
            SECTION_TYPE_PROFILE,
            SECTION_TYPE_EDUCATION,
            SECTION_TYPE_PROJECT,
            SECTION_TYPE_WORK,
            SECTION_TYPE_SKILL,
            SECTION_TYPE_INTRODUCTION,
            SECTION_TYPE_CUSTOM
    );

    public static final String WORK_TYPE_FULL_TIME = "full_time";
    public static final String WORK_TYPE_INTERNSHIP = "internship";
    public static final String WORK_TYPE_PART_TIME = "part_time";
    public static final String WORK_TYPE_CAMPUS_JOB = "campus_job";
    public static final String WORK_TYPE_RESEARCH_ASSISTANT = "research_assistant";
    public static final String WORK_TYPE_VOLUNTEER = "volunteer";

    public static final java.util.Set<String> WORK_TYPES = java.util.Set.of(
            WORK_TYPE_FULL_TIME,
            WORK_TYPE_INTERNSHIP,
            WORK_TYPE_PART_TIME,
            WORK_TYPE_CAMPUS_JOB,
            WORK_TYPE_RESEARCH_ASSISTANT,
            WORK_TYPE_VOLUNTEER
    );

    public static final String PROJECT_TYPE_RESEARCH = "research";
    public static final String PROJECT_TYPE_COURSE = "course";
    public static final String PROJECT_TYPE_ENTERPRISE = "enterprise";
    public static final String PROJECT_TYPE_COMPETITION = "competition";
    public static final String PROJECT_TYPE_OPEN_SOURCE = "open_source";
    public static final String PROJECT_TYPE_PERSONAL = "personal";
    public static final String PROJECT_TYPE_OTHER = "other";

    public static final java.util.Set<String> PROJECT_TYPES = java.util.Set.of(
            PROJECT_TYPE_RESEARCH,
            PROJECT_TYPE_COURSE,
            PROJECT_TYPE_ENTERPRISE,
            PROJECT_TYPE_COMPETITION,
            PROJECT_TYPE_OPEN_SOURCE,
            PROJECT_TYPE_PERSONAL,
            PROJECT_TYPE_OTHER
    );

    public static final String SKILL_CATEGORY_PROGRAMMING_LANGUAGE = "programming_language";
    public static final String SKILL_CATEGORY_FRONTEND = "frontend";
    public static final String SKILL_CATEGORY_BACKEND = "backend";
    public static final String SKILL_CATEGORY_DATABASE = "database";
    public static final String SKILL_CATEGORY_AI_DATA = "ai_data";
    public static final String SKILL_CATEGORY_DESIGN = "design";
    public static final String SKILL_CATEGORY_OFFICE = "office";
    public static final String SKILL_CATEGORY_LANGUAGE = "language";
    public static final String SKILL_CATEGORY_PROFESSIONAL_TOOL = "professional_tool";
    public static final String SKILL_CATEGORY_OTHER = "other";

    public static final java.util.Set<String> SKILL_CATEGORIES = java.util.Set.of(
            SKILL_CATEGORY_PROGRAMMING_LANGUAGE,
            SKILL_CATEGORY_FRONTEND,
            SKILL_CATEGORY_BACKEND,
            SKILL_CATEGORY_DATABASE,
            SKILL_CATEGORY_AI_DATA,
            SKILL_CATEGORY_DESIGN,
            SKILL_CATEGORY_OFFICE,
            SKILL_CATEGORY_LANGUAGE,
            SKILL_CATEGORY_PROFESSIONAL_TOOL,
            SKILL_CATEGORY_OTHER
    );

    public static final String SKILL_LEVEL_BEGINNER = "beginner";
    public static final String SKILL_LEVEL_FAMILIAR = "familiar";
    public static final String SKILL_LEVEL_PROFICIENT = "proficient";
    public static final String SKILL_LEVEL_EXPERT = "expert";

    public static final java.util.Set<String> SKILL_LEVELS = java.util.Set.of(
            SKILL_LEVEL_BEGINNER,
            SKILL_LEVEL_FAMILIAR,
            SKILL_LEVEL_PROFICIENT,
            SKILL_LEVEL_EXPERT
    );

    public static final String INTRODUCTION_STYLE_CONCISE_FORMAL = "concise_formal";
    public static final String INTRODUCTION_STYLE_TECH_ORIENTED = "tech_oriented";
    public static final String INTRODUCTION_STYLE_STUDENT = "student";
    public static final String INTRODUCTION_STYLE_SENIOR = "senior";
    public static final String INTRODUCTION_STYLE_POSTGRADUATE = "postgraduate";
    public static final String INTRODUCTION_STYLE_PROJECT = "project";

    public static final java.util.Set<String> INTRODUCTION_STYLES = java.util.Set.of(
            INTRODUCTION_STYLE_CONCISE_FORMAL,
            INTRODUCTION_STYLE_TECH_ORIENTED,
            INTRODUCTION_STYLE_STUDENT,
            INTRODUCTION_STYLE_SENIOR,
            INTRODUCTION_STYLE_POSTGRADUATE,
            INTRODUCTION_STYLE_PROJECT
    );
}
