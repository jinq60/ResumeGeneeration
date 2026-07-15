package com.resume.resume.service;

import com.resume.common.constant.BizConstant;
import com.resume.common.constant.ResultCode;
import com.resume.common.enums.SectionType;
import com.resume.common.exception.BusinessException;
import com.resume.resume.dto.SectionDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 简历 Section 校验器。
 * <p>
 * 提供两层校验：
 * <ul>
 *   <li>草稿/自动保存：结构 + 字段格式（字段缺失不报错）。</li>
 *   <li>导出/PDF：结构 + 字段格式 + 必填与跨字段规则。</li>
 * </ul>
 */
@Component
public class ResumeSectionValidator {

    private static final Pattern PHONE = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern EMAIL = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern URL = Pattern.compile("^https?://([\\w-]+\\.)+[\\w-]+(/[\\w-./?%&=]*)?$");
    private static final Pattern YYYY_MM = Pattern.compile("^(19|20)\\d{2}-(0[1-9]|1[0-2])$");
    private static final Pattern NAME = Pattern.compile("^[一-龥A-Za-z·]{1,50}$");

    private static final String PRESENT = "present";

    /**
     * 草稿级校验：字段存在时才校验格式，不强制完整。
     */
    public void validateDraft(List<SectionDTO> sections) {
        validateStructure(sections);
        for (SectionDTO section : sections) {
            validateSectionFields(section, false);
        }
    }

    /**
     * 导出/PDF 级校验：强制姓名与联系方式，并校验所有字段格式。
     */
    public void validateForExport(List<SectionDTO> sections) {
        validateStructure(sections);
        for (SectionDTO section : sections) {
            validateSectionFields(section, true);
        }
        validateProfileStrict(sections);
    }

    private void validateStructure(List<SectionDTO> sections) {
        if (sections == null) {
            throw new BusinessException(ResultCode.RESUME_SECTION_INVALID, "简历内容不能为空。");
        }
        Set<String> ids = new java.util.HashSet<>();
        for (SectionDTO section : sections) {
            if (StringUtils.isBlank(section.getId()) || StringUtils.isBlank(section.getType())
                    || StringUtils.isBlank(section.getTitle()) || section.getOrder() == null
                    || section.getOrder() < 0 || section.getVisible() == null || section.getData() == null) {
                throw new BusinessException(ResultCode.RESUME_SECTION_INVALID, "简历模块数据不正确。");
            }
            if (!Arrays.asList(BizConstant.SECTION_TYPES).contains(section.getType())) {
                throw new BusinessException(ResultCode.RESUME_SECTION_INVALID, "模块类型不正确。");
            }
            if (!ids.add(section.getId())) {
                throw new BusinessException(ResultCode.RESUME_SECTION_INVALID, "模块 ID 重复。");
            }
        }
    }

    private void validateSectionFields(SectionDTO section, boolean strict) {
        switch (section.getType()) {
            case BizConstant.SECTION_TYPE_PROFILE -> validateProfile(section.getData(), strict);
            case BizConstant.SECTION_TYPE_EDUCATION -> validateEducation(section.getData(), strict);
            case BizConstant.SECTION_TYPE_WORK -> validateWork(section.getData(), strict);
            case BizConstant.SECTION_TYPE_PROJECT -> validateProject(section.getData(), strict);
            case BizConstant.SECTION_TYPE_SKILL -> validateSkill(section.getData(), strict);
            case BizConstant.SECTION_TYPE_INTRODUCTION -> validateIntroduction(section.getData(), strict);
            default -> {
                // custom 模块不校验内部数据
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void validateProfile(Object data, boolean strict) {
        if (!(data instanceof Map)) {
            throw new BusinessException(ResultCode.RESUME_SECTION_INVALID, "个人信息模块数据格式不正确。");
        }
        Map<String, Object> profile = (Map<String, Object>) data;

        validateStringIfPresent(profile, "name", 50, "姓名过长。", false, 0, null);
        if (StringUtils.isNotBlank(getString(profile, "name")) && !NAME.matcher(getString(profile, "name")).matches()) {
            throw new BusinessException(ResultCode.RESUME_PROFILE_NAME_REQUIRED, "姓名格式不正确。");
        }

        validateEnumIfPresent(profile, "gender", Arrays.asList(BizConstant.GENDERS), "性别格式不正确。");
        validatePatternIfPresent(profile, "birthDate", YYYY_MM, "出生年月格式不正确。");
        validatePhoneIfPresent(profile);
        validateEmailIfPresent(profile, "email");
        validateStringIfPresent(profile, "city", 50, "所在城市过长。", false, 0, null);
        validateStringIfPresent(profile, "targetPosition", 128, "目标岗位过长。", false, 0, null);
        validateStringIfPresent(profile, "expectedSalary", 64, "期望薪资过长。", false, 0, null);
        validateStringIfPresent(profile, "availability", 64, "到岗时间描述过长。", false, 0, null);
        validateUrlIfPresent(profile, "personalWebsite");
        validateUrlIfPresent(profile, "github");
        validateUrlIfPresent(profile, "portfolio");
        validateUrlIfPresent(profile, "avatarUrl");
    }

    private void validateProfileStrict(List<SectionDTO> sections) {
        Map<String, Object> profile = findProfile(sections);
        String name = profile == null ? "" : getString(profile, "name");
        String phone = profile == null ? "" : getString(profile, "phone");
        String email = profile == null ? "" : getString(profile, "email");

        if (StringUtils.isBlank(name)) {
            throw new BusinessException(ResultCode.PDF_EXPORT_NAME_REQUIRED, "建议填写姓名，便于生成正式简历。");
        }
        if (StringUtils.isBlank(phone) && StringUtils.isBlank(email)) {
            throw new BusinessException(ResultCode.PDF_EXPORT_CONTACT_REQUIRED, "简历中至少需要填写手机号或邮箱。");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> findProfile(List<SectionDTO> sections) {
        return sections.stream()
                .filter(s -> BizConstant.SECTION_TYPE_PROFILE.equals(s.getType()))
                .filter(s -> s.getData() instanceof Map)
                .map(s -> (Map<String, Object>) s.getData())
                .findFirst()
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    private void validateEducation(Object data, boolean strict) {
        if (!(data instanceof List)) {
            throw new BusinessException(ResultCode.RESUME_SECTION_INVALID, "教育经历模块数据格式不正确。");
        }
        List<Object> items = (List<Object>) data;
        for (Object obj : items) {
            if (!(obj instanceof Map)) {
                throw new BusinessException(ResultCode.RESUME_SECTION_INVALID, "教育经历项数据格式不正确。");
            }
            Map<String, Object> item = (Map<String, Object>) obj;
            validateStringIfPresent(item, "school", 100, "学校名称过长。", strict, ResultCode.RESUME_SECTION_INVALID, "学校名称为必填项。");
            validateStringIfPresent(item, "degree", 32, "学历描述过长。", strict, ResultCode.RESUME_SECTION_INVALID, "学历为必填项。");
            validateStringIfPresent(item, "major", 100, "专业名称过长。", strict, ResultCode.RESUME_SECTION_INVALID, "专业为必填项。");
            validateStringIfPresent(item, "college", 100, "学院名称过长。", false, 0, null);
            validateDateRange(item, "入学时间格式不正确。", "毕业时间格式不正确。");
            validateStringIfPresent(item, "gpa", 32, "GPA 描述过长。", false, 0, null);
            validateStringIfPresent(item, "rank", 64, "排名描述过长。", false, 0, null);
            validateStringArray(item, "honors", 128, "荣誉描述过长。");
            validateStringArray(item, "courses", 64, "课程名称过长。");
        }
    }

    @SuppressWarnings("unchecked")
    private void validateWork(Object data, boolean strict) {
        if (!(data instanceof List)) {
            throw new BusinessException(ResultCode.RESUME_SECTION_INVALID, "工作经历模块数据格式不正确。");
        }
        List<Object> items = (List<Object>) data;
        for (Object obj : items) {
            if (!(obj instanceof Map)) {
                throw new BusinessException(ResultCode.RESUME_SECTION_INVALID, "工作经历项数据格式不正确。");
            }
            Map<String, Object> item = (Map<String, Object>) obj;
            validateStringIfPresent(item, "company", 128, "公司名称过长。", strict, ResultCode.RESUME_SECTION_INVALID, "公司名称为必填项。");
            validateStringIfPresent(item, "department", 64, "部门名称过长。", false, 0, null);
            validateStringIfPresent(item, "position", 64, "职位描述过长。", strict, ResultCode.RESUME_SECTION_INVALID, "职位为必填项。");
            validateEnumIfPresent(item, "type", Arrays.asList(BizConstant.WORK_TYPES), "工作类型不正确。");
            validateStringIfPresent(item, "city", 50, "工作城市过长。", false, 0, null);
            validateDateRange(item, "入职时间格式不正确。", "离职时间格式不正确。");
            validateStringList(item, "description", 1, 8, 200, strict, "工作内容至少需要 1 条。", "工作内容描述过长。");
            validateStringArray(item, "achievements", 200, "工作成果描述过长。");
            validateStringArray(item, "techStack", 32, "技术栈标签过长。");
            validateStringIfPresent(item, "leaveReason", 200, "离职原因描述过长。", false, 0, null);
        }
    }

    @SuppressWarnings("unchecked")
    private void validateProject(Object data, boolean strict) {
        if (!(data instanceof List)) {
            throw new BusinessException(ResultCode.RESUME_SECTION_INVALID, "项目经历模块数据格式不正确。");
        }
        List<Object> items = (List<Object>) data;
        for (Object obj : items) {
            if (!(obj instanceof Map)) {
                throw new BusinessException(ResultCode.RESUME_SECTION_INVALID, "项目经历项数据格式不正确。");
            }
            Map<String, Object> item = (Map<String, Object>) obj;
            validateStringIfPresent(item, "name", 128, "项目名称过长。", strict, ResultCode.RESUME_SECTION_INVALID, "项目名称为必填项。");
            validateStringIfPresent(item, "role", 64, "项目角色描述过长。", false, 0, null);
            validateEnumIfPresent(item, "type", Arrays.asList(BizConstant.PROJECT_TYPES), "项目类型不正确。");
            validateDateRange(item, "项目开始时间格式不正确。", "项目结束时间格式不正确。");
            validateStringArray(item, "techStack", 32, "技术栈标签过长。");
            validateStringIfPresent(item, "background", 500, "项目背景描述过长。", false, 0, null);
            validateStringIfPresent(item, "responsibility", 500, "个人职责描述过长。", false, 0, null);
            validateStringArray(item, "achievements", 200, "项目成果描述过长。");
            validateStringList(item, "description", 1, 8, 200, strict, "项目描述至少需要 1 条。", "项目描述过长。");
            validateUrlIfPresent(item, "link");
            validateUrlIfPresent(item, "github");
        }
    }

    @SuppressWarnings("unchecked")
    private void validateSkill(Object data, boolean strict) {
        if (!(data instanceof List)) {
            throw new BusinessException(ResultCode.RESUME_SECTION_INVALID, "技能模块数据格式不正确。");
        }
        List<Object> items = (List<Object>) data;
        if (strict && items.isEmpty()) {
            throw new BusinessException(ResultCode.RESUME_SECTION_INVALID, "技能项不能为空。");
        }
        for (Object obj : items) {
            if (!(obj instanceof Map)) {
                throw new BusinessException(ResultCode.RESUME_SECTION_INVALID, "技能项数据格式不正确。");
            }
            Map<String, Object> item = (Map<String, Object>) obj;
            validateEnumIfPresent(item, "category", Arrays.asList(BizConstant.SKILL_CATEGORIES), "技能分类不正确。");

            Object rawItems = item.get("items");
            if (!(rawItems instanceof List)) {
                if (strict) {
                    throw new BusinessException(ResultCode.RESUME_SECTION_INVALID, "技能项不能为空。");
                }
                continue;
            }
            List<Object> skills = (List<Object>) rawItems;
            if (strict && skills.isEmpty()) {
                throw new BusinessException(ResultCode.RESUME_SECTION_INVALID, "技能项不能为空。");
            }
            if (skills.size() > 20) {
                throw new BusinessException(ResultCode.RESUME_SECTION_INVALID, "技能项数量不能超过 20 个。");
            }
            for (Object skillObj : skills) {
                if (!(skillObj instanceof Map)) {
                    throw new BusinessException(ResultCode.RESUME_SECTION_INVALID, "技能项数据格式不正确。");
                }
                Map<String, Object> skill = (Map<String, Object>) skillObj;
                validateStringIfPresent(skill, "name", 64, "技能名称过长。", strict, ResultCode.RESUME_SECTION_INVALID, "技能名称为必填项。");
                validateEnumIfPresent(skill, "level", Arrays.asList(BizConstant.SKILL_LEVELS), "熟练程度不正确。");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void validateIntroduction(Object data, boolean strict) {
        if (!(data instanceof Map)) {
            throw new BusinessException(ResultCode.RESUME_SECTION_INVALID, "自我介绍模块数据格式不正确。");
        }
        Map<String, Object> item = (Map<String, Object>) data;
        validateStringIfPresent(item, "content", 500, "自我介绍过长。", strict, ResultCode.RESUME_SECTION_INVALID, "自我介绍为必填项。");
        validateStringArray(item, "keywords", 20, "关键词过长。");
        validateEnumIfPresent(item, "style", Arrays.asList(BizConstant.INTRODUCTION_STYLES), "自我介绍风格不正确。");

        Object maxWords = item.get("maxWords");
        if (maxWords instanceof Number n && n.intValue() > 500) {
            throw new BusinessException(ResultCode.RESUME_SECTION_INVALID, "字数限制过大。");
        }
    }

    private void validateStringIfPresent(Map<String, Object> map, String key, int maxLen,
                                         String tooLongMessage, boolean required, int requiredCode, String requiredMessage) {
        Object value = map.get(key);
        if (value == null || (value instanceof String s && StringUtils.isBlank(s))) {
            if (required) {
                throw new BusinessException(requiredCode, requiredMessage);
            }
            return;
        }
        if (!(value instanceof String s)) {
            throw new BusinessException(ResultCode.RESUME_SECTION_INVALID, key + " 格式不正确。");
        }
        if (s.length() > maxLen) {
            throw new BusinessException(ResultCode.RESUME_SECTION_INVALID, tooLongMessage);
        }
    }

    private void validatePatternIfPresent(Map<String, Object> map, String key, Pattern pattern, String message) {
        String value = getString(map, key);
        if (StringUtils.isBlank(value)) {
            return;
        }
        if (!pattern.matcher(value).matches()) {
            throw new BusinessException(ResultCode.RESUME_SECTION_INVALID, message);
        }
    }

    private void validateEmailIfPresent(Map<String, Object> map, String key) {
        String value = getString(map, key);
        if (StringUtils.isBlank(value)) {
            return;
        }
        if (!EMAIL.matcher(value).matches()) {
            throw new BusinessException(ResultCode.RESUME_PROFILE_EMAIL_INVALID, "邮箱格式不正确。");
        }
        if (value.length() > 128) {
            throw new BusinessException(ResultCode.RESUME_SECTION_INVALID, "邮箱过长。");
        }
    }

    private void validatePhoneIfPresent(Map<String, Object> map) {
        String value = getString(map, "phone");
        if (StringUtils.isBlank(value)) {
            return;
        }
        if (!PHONE.matcher(value).matches()) {
            throw new BusinessException(ResultCode.RESUME_PROFILE_PHONE_INVALID, "手机号格式不正确。");
        }
    }

    private void validateUrlIfPresent(Map<String, Object> map, String key) {
        String value = getString(map, key);
        if (StringUtils.isBlank(value)) {
            return;
        }
        if (value.length() > 512 || !URL.matcher(value).matches()) {
            throw new BusinessException(ResultCode.RESUME_PROFILE_URL_INVALID, "请输入正确的网址格式。");
        }
    }

    private void validateEnumIfPresent(Map<String, Object> map, String key, Collection<String> values, String message) {
        String value = getString(map, key);
        if (StringUtils.isBlank(value)) {
            return;
        }
        if (!values.contains(value)) {
            throw new BusinessException(ResultCode.RESUME_SECTION_INVALID, message);
        }
    }

    private void validateDateRange(Map<String, Object> item, String startMessage, String endMessage) {
        String startDate = getString(item, "startDate");
        String endDate = getString(item, "endDate");
        if (StringUtils.isNotBlank(startDate) && !YYYY_MM.matcher(startDate).matches()) {
            throw new BusinessException(ResultCode.RESUME_SECTION_INVALID, startMessage);
        }
        if (StringUtils.isNotBlank(endDate) && !PRESENT.equals(endDate) && !YYYY_MM.matcher(endDate).matches()) {
            throw new BusinessException(ResultCode.RESUME_SECTION_INVALID, endMessage);
        }
        if (StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)
                && !PRESENT.equals(endDate) && endDate.compareTo(startDate) < 0) {
            throw new BusinessException(ResultCode.RESUME_SECTION_INVALID, "结束时间不能早于开始时间。");
        }
    }

    @SuppressWarnings("unchecked")
    private void validateStringArray(Map<String, Object> item, String key, int maxLen, String tooLongMessage) {
        Object value = item.get(key);
        if (value == null) {
            return;
        }
        if (!(value instanceof List)) {
            throw new BusinessException(ResultCode.RESUME_SECTION_INVALID, key + " 格式不正确。");
        }
        for (Object obj : (List<Object>) value) {
            if (!(obj instanceof String s)) {
                throw new BusinessException(ResultCode.RESUME_SECTION_INVALID, key + " 格式不正确。");
            }
            if (s.length() > maxLen) {
                throw new BusinessException(ResultCode.RESUME_SECTION_INVALID, tooLongMessage);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void validateStringList(Map<String, Object> item, String key, int minSize, int maxSize, int maxLen,
                                    boolean required, String requiredMessage, String tooLongMessage) {
        Object value = item.get(key);
        if (value == null || (value instanceof List list && list.isEmpty())) {
            if (required) {
                throw new BusinessException(ResultCode.RESUME_SECTION_INVALID, requiredMessage);
            }
            return;
        }
        if (!(value instanceof List)) {
            throw new BusinessException(ResultCode.RESUME_SECTION_INVALID, key + " 格式不正确。");
        }
        List<Object> list = (List<Object>) value;
        if (list.size() > maxSize) {
            throw new BusinessException(ResultCode.RESUME_SECTION_INVALID, key + " 数量不能超过 " + maxSize + " 条。");
        }
        if (required && list.size() < minSize) {
            throw new BusinessException(ResultCode.RESUME_SECTION_INVALID, requiredMessage);
        }
        for (Object obj : list) {
            if (!(obj instanceof String s)) {
                throw new BusinessException(ResultCode.RESUME_SECTION_INVALID, key + " 格式不正确。");
            }
            if (s.length() > maxLen) {
                throw new BusinessException(ResultCode.RESUME_SECTION_INVALID, tooLongMessage);
            }
        }
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }
}
