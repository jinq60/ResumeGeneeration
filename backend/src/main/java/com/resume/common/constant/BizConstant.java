package com.resume.common.constant;

/**
 * 业务状态与枚举常量。
 */
public final class BizConstant {

    private BizConstant() {
    }

    /**
     * 用户状态。
     */
    public static final String USER_STATUS_ACTIVE = "active";
    public static final String USER_STATUS_DISABLED = "disabled";

    /**
     * 简历状态。
     */
    public static final String RESUME_STATUS_ACTIVE = "active";
    public static final String RESUME_STATUS_DELETED = "deleted";

    /**
     * 模板状态。
     */
    public static final String TEMPLATE_STATUS_ACTIVE = "active";
    public static final String TEMPLATE_STATUS_INACTIVE = "inactive";

    /**
     * 任务状态。
     */
    public static final String TASK_STATUS_PENDING = "pending";
    public static final String TASK_STATUS_PROCESSING = "processing";
    public static final String TASK_STATUS_SUCCESS = "success";
    public static final String TASK_STATUS_FAILED = "failed";

    /**
     * 点评状态。
     */
    public static final String REVIEW_STATUS_PENDING = "pending";
    public static final String REVIEW_STATUS_SUCCESS = "success";
    public static final String REVIEW_STATUS_FAILED = "failed";

    /**
     * 性别。
     */
    public static final String GENDER_MALE = "male";
    public static final String GENDER_FEMALE = "female";
    public static final String GENDER_OTHER = "other";

    public static final String[] GENDERS = {
            GENDER_MALE,
            GENDER_FEMALE,
            GENDER_OTHER
    };

    /**
     * 简历使用场景。
     */
    public static final String SCENE_CAMPUS_RECRUITMENT = "campus_recruitment";
    public static final String SCENE_INTERNSHIP = "internship";
    public static final String SCENE_SOCIAL_RECRUITMENT = "social_recruitment";
    public static final String SCENE_POSTGRADUATE_REEXAM = "postgraduate_reexam";
    public static final String SCENE_PROJECT_APPLICATION = "project_application";
    public static final String SCENE_CUSTOM = "custom";

    public static final String[] SCENES = {
            SCENE_CAMPUS_RECRUITMENT,
            SCENE_INTERNSHIP,
            SCENE_SOCIAL_RECRUITMENT,
            SCENE_POSTGRADUATE_REEXAM,
            SCENE_PROJECT_APPLICATION,
            SCENE_CUSTOM
    };

    /**
     * Section 类型。
     */
    public static final String SECTION_TYPE_PROFILE = "profile";
    public static final String SECTION_TYPE_EDUCATION = "education";
    public static final String SECTION_TYPE_PROJECT = "project";
    public static final String SECTION_TYPE_WORK = "work";
    public static final String SECTION_TYPE_SKILL = "skill";
    public static final String SECTION_TYPE_INTRODUCTION = "introduction";
    public static final String SECTION_TYPE_CUSTOM = "custom";

    public static final String[] SECTION_TYPES = {
            SECTION_TYPE_PROFILE,
            SECTION_TYPE_EDUCATION,
            SECTION_TYPE_PROJECT,
            SECTION_TYPE_WORK,
            SECTION_TYPE_SKILL,
            SECTION_TYPE_INTRODUCTION,
            SECTION_TYPE_CUSTOM
    };

    /**
     * 工作类型。
     */
    public static final String WORK_TYPE_FULL_TIME = "full_time";
    public static final String WORK_TYPE_INTERNSHIP = "internship";
    public static final String WORK_TYPE_PART_TIME = "part_time";
    public static final String WORK_TYPE_CAMPUS_JOB = "campus_job";
    public static final String WORK_TYPE_RESEARCH_ASSISTANT = "research_assistant";
    public static final String WORK_TYPE_VOLUNTEER = "volunteer";

    public static final String[] WORK_TYPES = {
            WORK_TYPE_FULL_TIME,
            WORK_TYPE_INTERNSHIP,
            WORK_TYPE_PART_TIME,
            WORK_TYPE_CAMPUS_JOB,
            WORK_TYPE_RESEARCH_ASSISTANT,
            WORK_TYPE_VOLUNTEER
    };

    /**
     * 项目类型。
     */
    public static final String PROJECT_TYPE_RESEARCH = "research";
    public static final String PROJECT_TYPE_COURSE = "course";
    public static final String PROJECT_TYPE_ENTERPRISE = "enterprise";
    public static final String PROJECT_TYPE_COMPETITION = "competition";
    public static final String PROJECT_TYPE_OPEN_SOURCE = "open_source";
    public static final String PROJECT_TYPE_PERSONAL = "personal";
    public static final String PROJECT_TYPE_OTHER = "other";

    public static final String[] PROJECT_TYPES = {
            PROJECT_TYPE_RESEARCH,
            PROJECT_TYPE_COURSE,
            PROJECT_TYPE_ENTERPRISE,
            PROJECT_TYPE_COMPETITION,
            PROJECT_TYPE_OPEN_SOURCE,
            PROJECT_TYPE_PERSONAL,
            PROJECT_TYPE_OTHER
    };

    /**
     * 技能分类。
     */
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

    public static final String[] SKILL_CATEGORIES = {
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
    };

    /**
     * 技能熟练程度。
     */
    public static final String SKILL_LEVEL_BEGINNER = "beginner";
    public static final String SKILL_LEVEL_FAMILIAR = "familiar";
    public static final String SKILL_LEVEL_PROFICIENT = "proficient";
    public static final String SKILL_LEVEL_EXPERT = "expert";

    public static final String[] SKILL_LEVELS = {
            SKILL_LEVEL_BEGINNER,
            SKILL_LEVEL_FAMILIAR,
            SKILL_LEVEL_PROFICIENT,
            SKILL_LEVEL_EXPERT
    };

    /**
     * 自我介绍风格。
     */
    public static final String INTRODUCTION_STYLE_CONCISE_FORMAL = "concise_formal";
    public static final String INTRODUCTION_STYLE_TECH_ORIENTED = "tech_oriented";
    public static final String INTRODUCTION_STYLE_STUDENT = "student";
    public static final String INTRODUCTION_STYLE_SENIOR = "senior";
    public static final String INTRODUCTION_STYLE_POSTGRADUATE = "postgraduate";
    public static final String INTRODUCTION_STYLE_PROJECT = "project";

    public static final String[] INTRODUCTION_STYLES = {
            INTRODUCTION_STYLE_CONCISE_FORMAL,
            INTRODUCTION_STYLE_TECH_ORIENTED,
            INTRODUCTION_STYLE_STUDENT,
            INTRODUCTION_STYLE_SENIOR,
            INTRODUCTION_STYLE_POSTGRADUATE,
            INTRODUCTION_STYLE_PROJECT
    };

    /**
     * 头像背景类型：P0 仅支持白/蓝/红，transparent 为 P2 预留。
     */
    public static final String AVATAR_BACKGROUND_WHITE = "white";
    public static final String AVATAR_BACKGROUND_BLUE = "blue";
    public static final String AVATAR_BACKGROUND_RED = "red";
    public static final String AVATAR_BACKGROUND_TRANSPARENT = "transparent";

    public static final String[] AVATAR_BACKGROUND_TYPES_P0 = {
            AVATAR_BACKGROUND_WHITE,
            AVATAR_BACKGROUND_BLUE,
            AVATAR_BACKGROUND_RED
    };

    public static final String[] AVATAR_BACKGROUND_TYPES_ALL = {
            AVATAR_BACKGROUND_WHITE,
            AVATAR_BACKGROUND_BLUE,
            AVATAR_BACKGROUND_RED,
            AVATAR_BACKGROUND_TRANSPARENT
    };

    /**
     * 头像风格。
     */
    public static final String AVATAR_STYLE_FORMAL = "formal";
    public static final String AVATAR_STYLE_NATURAL = "natural";
    public static final String AVATAR_STYLE_PROFESSIONAL = "professional";

    public static final String[] AVATAR_STYLES = {
            AVATAR_STYLE_FORMAL,
            AVATAR_STYLE_NATURAL,
            AVATAR_STYLE_PROFESSIONAL
    };

    /**
     * 渲染引擎。
     */
    public static final String RENDER_ENGINE_SERVER = "server";
    public static final String RENDER_ENGINE_CLIENT = "client";
    public static final String RENDER_ENGINE_HYBRID = "hybrid";

    /**
     * 逻辑删除标记。
     */
    public static final Integer DELETED = 1;
    public static final Integer NOT_DELETED = 0;

    /**
     * 游客标记。
     */
    public static final Integer IS_GUEST = 1;
    public static final Integer IS_NOT_GUEST = 0;

    /**
     * 系统内置模板。
     */
    public static final Integer BUILTIN_YES = 1;
    public static final Integer BUILTIN_NO = 0;
}
