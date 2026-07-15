package com.resume.resume.dto.section;

import lombok.Data;

/**
 * Section 数据包装类，支持不同类型的模块数据。
 * 根据模块类型，只有一个字段会被填充。
 */
@Data
public class SectionData {

    /**
     * 个人信息数据。
     */
    private ProfileData profile;

    /**
     * 教育经历数据。
     */
    private EducationData education;

    /**
     * 项目经历数据。
     */
    private ProjectData project;

    /**
     * 工作经历数据。
     */
    private WorkExperienceData work;

    /**
     * 技能数据。
     */
    private SkillData skill;

    /**
     * 自我介绍数据。
     */
    private IntroductionData introduction;

    /**
     * 自定义模块数据。
     */
    private CustomData custom;
}
