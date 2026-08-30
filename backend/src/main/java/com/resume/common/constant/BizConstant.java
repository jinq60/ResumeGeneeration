package com.resume.common.constant;

/**
 * 业务状态与枚举常量（兼容代理）。
 * <p>
 * 历史单体常量类，现已按领域拆分为：
 * {@link UserConstant}、{@link ResumeConstant}、{@link TemplateConstant}、
 * {@link AvatarConstant}、{@link TaskConstant}、{@link CommonConstant}。
 * 本类保留常量代理以保证既有代码兼容，新代码请直接引用领域常量类。
 * </p>
 */
public final class BizConstant {

    private BizConstant() {
    }

    // ——— 用户 ———
    public static final String USER_STATUS_ACTIVE = UserConstant.USER_STATUS_ACTIVE;
    public static final String USER_STATUS_DISABLED = UserConstant.USER_STATUS_DISABLED;
    public static final String USER_ROLE_USER = UserConstant.USER_ROLE_USER;
    public static final String USER_ROLE_ADMIN = UserConstant.USER_ROLE_ADMIN;
    public static final Integer IS_GUEST = UserConstant.IS_GUEST;
    public static final Integer IS_NOT_GUEST = UserConstant.IS_NOT_GUEST;
    public static final String GENDER_MALE = UserConstant.GENDER_MALE;
    public static final String GENDER_FEMALE = UserConstant.GENDER_FEMALE;
    public static final String GENDER_OTHER = UserConstant.GENDER_OTHER;
    public static final java.util.Set<String> GENDERS = UserConstant.GENDERS;

    // ——— 简历 ———
    public static final String RESUME_STATUS_ACTIVE = ResumeConstant.RESUME_STATUS_ACTIVE;
    public static final String RESUME_STATUS_DELETED = ResumeConstant.RESUME_STATUS_DELETED;
    public static final String SCENE_CAMPUS_RECRUITMENT = ResumeConstant.SCENE_CAMPUS_RECRUITMENT;
    public static final String SCENE_INTERNSHIP = ResumeConstant.SCENE_INTERNSHIP;
    public static final String SCENE_SOCIAL_RECRUITMENT = ResumeConstant.SCENE_SOCIAL_RECRUITMENT;
    public static final String SCENE_POSTGRADUATE_REEXAM = ResumeConstant.SCENE_POSTGRADUATE_REEXAM;
    public static final String SCENE_PROJECT_APPLICATION = ResumeConstant.SCENE_PROJECT_APPLICATION;
    public static final String SCENE_CUSTOM = ResumeConstant.SCENE_CUSTOM;
    public static final java.util.Set<String> SCENES = ResumeConstant.SCENES;
    public static final String SECTION_TYPE_PROFILE = ResumeConstant.SECTION_TYPE_PROFILE;
    public static final String SECTION_TYPE_EDUCATION = ResumeConstant.SECTION_TYPE_EDUCATION;
    public static final String SECTION_TYPE_PROJECT = ResumeConstant.SECTION_TYPE_PROJECT;
    public static final String SECTION_TYPE_WORK = ResumeConstant.SECTION_TYPE_WORK;
    public static final String SECTION_TYPE_SKILL = ResumeConstant.SECTION_TYPE_SKILL;
    public static final String SECTION_TYPE_INTRODUCTION = ResumeConstant.SECTION_TYPE_INTRODUCTION;
    public static final String SECTION_TYPE_CUSTOM = ResumeConstant.SECTION_TYPE_CUSTOM;
    public static final java.util.Set<String> SECTION_TYPES = ResumeConstant.SECTION_TYPES;
    public static final String WORK_TYPE_FULL_TIME = ResumeConstant.WORK_TYPE_FULL_TIME;
    public static final String WORK_TYPE_INTERNSHIP = ResumeConstant.WORK_TYPE_INTERNSHIP;
    public static final String WORK_TYPE_PART_TIME = ResumeConstant.WORK_TYPE_PART_TIME;
    public static final String WORK_TYPE_CAMPUS_JOB = ResumeConstant.WORK_TYPE_CAMPUS_JOB;
    public static final String WORK_TYPE_RESEARCH_ASSISTANT = ResumeConstant.WORK_TYPE_RESEARCH_ASSISTANT;
    public static final String WORK_TYPE_VOLUNTEER = ResumeConstant.WORK_TYPE_VOLUNTEER;
    public static final java.util.Set<String> WORK_TYPES = ResumeConstant.WORK_TYPES;
    public static final String PROJECT_TYPE_RESEARCH = ResumeConstant.PROJECT_TYPE_RESEARCH;
    public static final String PROJECT_TYPE_COURSE = ResumeConstant.PROJECT_TYPE_COURSE;
    public static final String PROJECT_TYPE_ENTERPRISE = ResumeConstant.PROJECT_TYPE_ENTERPRISE;
    public static final String PROJECT_TYPE_COMPETITION = ResumeConstant.PROJECT_TYPE_COMPETITION;
    public static final String PROJECT_TYPE_OPEN_SOURCE = ResumeConstant.PROJECT_TYPE_OPEN_SOURCE;
    public static final String PROJECT_TYPE_PERSONAL = ResumeConstant.PROJECT_TYPE_PERSONAL;
    public static final String PROJECT_TYPE_OTHER = ResumeConstant.PROJECT_TYPE_OTHER;
    public static final java.util.Set<String> PROJECT_TYPES = ResumeConstant.PROJECT_TYPES;
    public static final String SKILL_CATEGORY_PROGRAMMING_LANGUAGE = ResumeConstant.SKILL_CATEGORY_PROGRAMMING_LANGUAGE;
    public static final String SKILL_CATEGORY_FRONTEND = ResumeConstant.SKILL_CATEGORY_FRONTEND;
    public static final String SKILL_CATEGORY_BACKEND = ResumeConstant.SKILL_CATEGORY_BACKEND;
    public static final String SKILL_CATEGORY_DATABASE = ResumeConstant.SKILL_CATEGORY_DATABASE;
    public static final String SKILL_CATEGORY_AI_DATA = ResumeConstant.SKILL_CATEGORY_AI_DATA;
    public static final String SKILL_CATEGORY_DESIGN = ResumeConstant.SKILL_CATEGORY_DESIGN;
    public static final String SKILL_CATEGORY_OFFICE = ResumeConstant.SKILL_CATEGORY_OFFICE;
    public static final String SKILL_CATEGORY_LANGUAGE = ResumeConstant.SKILL_CATEGORY_LANGUAGE;
    public static final String SKILL_CATEGORY_PROFESSIONAL_TOOL = ResumeConstant.SKILL_CATEGORY_PROFESSIONAL_TOOL;
    public static final String SKILL_CATEGORY_OTHER = ResumeConstant.SKILL_CATEGORY_OTHER;
    public static final java.util.Set<String> SKILL_CATEGORIES = ResumeConstant.SKILL_CATEGORIES;
    public static final String SKILL_LEVEL_BEGINNER = ResumeConstant.SKILL_LEVEL_BEGINNER;
    public static final String SKILL_LEVEL_FAMILIAR = ResumeConstant.SKILL_LEVEL_FAMILIAR;
    public static final String SKILL_LEVEL_PROFICIENT = ResumeConstant.SKILL_LEVEL_PROFICIENT;
    public static final String SKILL_LEVEL_EXPERT = ResumeConstant.SKILL_LEVEL_EXPERT;
    public static final java.util.Set<String> SKILL_LEVELS = ResumeConstant.SKILL_LEVELS;
    public static final String INTRODUCTION_STYLE_CONCISE_FORMAL = ResumeConstant.INTRODUCTION_STYLE_CONCISE_FORMAL;
    public static final String INTRODUCTION_STYLE_TECH_ORIENTED = ResumeConstant.INTRODUCTION_STYLE_TECH_ORIENTED;
    public static final String INTRODUCTION_STYLE_STUDENT = ResumeConstant.INTRODUCTION_STYLE_STUDENT;
    public static final String INTRODUCTION_STYLE_SENIOR = ResumeConstant.INTRODUCTION_STYLE_SENIOR;
    public static final String INTRODUCTION_STYLE_POSTGRADUATE = ResumeConstant.INTRODUCTION_STYLE_POSTGRADUATE;
    public static final String INTRODUCTION_STYLE_PROJECT = ResumeConstant.INTRODUCTION_STYLE_PROJECT;
    public static final java.util.Set<String> INTRODUCTION_STYLES = ResumeConstant.INTRODUCTION_STYLES;

    // ——— 模板 ———
    public static final String TEMPLATE_STATUS_ACTIVE = TemplateConstant.TEMPLATE_STATUS_ACTIVE;
    public static final String TEMPLATE_STATUS_INACTIVE = TemplateConstant.TEMPLATE_STATUS_INACTIVE;
    public static final String RENDER_ENGINE_SERVER = TemplateConstant.RENDER_ENGINE_SERVER;
    public static final String RENDER_ENGINE_CLIENT = TemplateConstant.RENDER_ENGINE_CLIENT;
    public static final String RENDER_ENGINE_HYBRID = TemplateConstant.RENDER_ENGINE_HYBRID;
    public static final Integer BUILTIN_YES = CommonConstant.BUILTIN_YES;
    public static final Integer BUILTIN_NO = CommonConstant.BUILTIN_NO;

    // ——— 头像 ———
    public static final String AVATAR_BACKGROUND_WHITE = AvatarConstant.AVATAR_BACKGROUND_WHITE;
    public static final String AVATAR_BACKGROUND_BLUE = AvatarConstant.AVATAR_BACKGROUND_BLUE;
    public static final String AVATAR_BACKGROUND_RED = AvatarConstant.AVATAR_BACKGROUND_RED;
    public static final String AVATAR_BACKGROUND_TRANSPARENT = AvatarConstant.AVATAR_BACKGROUND_TRANSPARENT;
    public static final java.util.Set<String> AVATAR_BACKGROUND_TYPES_P0 = AvatarConstant.AVATAR_BACKGROUND_TYPES_P0;
    public static final java.util.Set<String> AVATAR_BACKGROUND_TYPES_ALL = AvatarConstant.AVATAR_BACKGROUND_TYPES_ALL;
    public static final String AVATAR_STYLE_FORMAL = AvatarConstant.AVATAR_STYLE_FORMAL;
    public static final String AVATAR_STYLE_NATURAL = AvatarConstant.AVATAR_STYLE_NATURAL;
    public static final String AVATAR_STYLE_PROFESSIONAL = AvatarConstant.AVATAR_STYLE_PROFESSIONAL;
    public static final java.util.Set<String> AVATAR_STYLES = AvatarConstant.AVATAR_STYLES;

    // ——— 任务/点评 ———
    public static final String TASK_STATUS_PENDING = TaskConstant.TASK_STATUS_PENDING;
    public static final String TASK_STATUS_PROCESSING = TaskConstant.TASK_STATUS_PROCESSING;
    public static final String TASK_STATUS_SUCCESS = TaskConstant.TASK_STATUS_SUCCESS;
    public static final String TASK_STATUS_FAILED = TaskConstant.TASK_STATUS_FAILED;
    public static final String REVIEW_STATUS_PENDING = TaskConstant.REVIEW_STATUS_PENDING;
    public static final String REVIEW_STATUS_SUCCESS = TaskConstant.REVIEW_STATUS_SUCCESS;
    public static final String REVIEW_STATUS_FAILED = TaskConstant.REVIEW_STATUS_FAILED;

    // ——— 通用 ———
    public static final Integer DELETED = CommonConstant.DELETED;
    public static final Integer NOT_DELETED = CommonConstant.NOT_DELETED;
}
