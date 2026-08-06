package com.resume.resume.service;

import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.resume.dto.SectionDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ResumeSectionValidatorTest {

    private ResumeSectionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ResumeSectionValidator();
    }

    @Test
    void draft_shouldAcceptEmptyProfile() {
        SectionDTO profile = createSection("profile", "个人信息", 0, new HashMap<String, Object>());
        assertDoesNotThrow(() -> validator.validateDraft(List.of(profile)));
    }

    @Test
    void draft_shouldRejectInvalidPhone() {
        Map<String, Object> data = new HashMap<>();
        data.put("phone", "12345678901");
        SectionDTO profile = createSection("profile", "个人信息", 0, data);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> validator.validateDraft(List.of(profile)));
        assertEquals(ResultCode.RESUME_PROFILE_PHONE_INVALID, ex.getErrorCode());
    }

    @Test
    void draft_shouldRejectInvalidEmail() {
        Map<String, Object> data = new HashMap<>();
        data.put("email", "not-an-email");
        SectionDTO profile = createSection("profile", "个人信息", 0, data);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> validator.validateDraft(List.of(profile)));
        assertEquals(ResultCode.RESUME_PROFILE_EMAIL_INVALID, ex.getErrorCode());
    }

    @Test
    void draft_shouldRejectInvalidUrl() {
        Map<String, Object> data = new HashMap<>();
        data.put("github", "ftp://example.com/repo");
        SectionDTO profile = createSection("profile", "个人信息", 0, data);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> validator.validateDraft(List.of(profile)));
        assertEquals(ResultCode.RESUME_PROFILE_URL_INVALID, ex.getErrorCode());
    }

    @Test
    void draft_shouldRejectInvertedWorkDates() {
        Map<String, Object> work = new HashMap<>();
        work.put("company", "A 公司");
        work.put("position", "工程师");
        work.put("startDate", "2022-01");
        work.put("endDate", "2021-06");
        SectionDTO section = createSection("work", "工作经历", 0, List.of(work));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> validator.validateDraft(List.of(section)));
        assertEquals(ResultCode.RESUME_SECTION_INVALID, ex.getErrorCode());
    }

    @Test
    void export_shouldRejectMissingName() {
        SectionDTO profile = createSection("profile", "个人信息", 0, new HashMap<String, Object>());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> validator.validateForExport(List.of(profile)));
        assertEquals(ResultCode.PDF_EXPORT_NAME_REQUIRED, ex.getErrorCode());
    }

    @Test
    void export_shouldRejectMissingContact() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "张三");
        SectionDTO profile = createSection("profile", "个人信息", 0, data);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> validator.validateForExport(List.of(profile)));
        assertEquals(ResultCode.PDF_EXPORT_CONTACT_REQUIRED, ex.getErrorCode());
    }

    @Test
    void export_shouldAcceptValidProfile() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "张三");
        data.put("phone", "13800000000");
        SectionDTO profile = createSection("profile", "个人信息", 0, data);

        assertDoesNotThrow(() -> validator.validateForExport(List.of(profile)));
    }

    @Test
    void draft_shouldRejectOverlongRichText() {
        Map<String, Object> intro = new HashMap<>();
        intro.put("content", "简短文本");
        intro.put("contentHtml", "<p>" + "x".repeat(501) + "</p>");
        SectionDTO section = createSection("introduction", "自我介绍", 0, intro);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> validator.validateDraft(List.of(section)));
        assertEquals(ResultCode.RESUME_SECTION_INVALID, ex.getErrorCode());
    }

    @Test
    void export_shouldAcceptRichTextWithoutLegacyPlainTextField() {
        Map<String, Object> intro = new HashMap<>();
        intro.put("contentHtml", "<p>富文本自我介绍</p>");
        SectionDTO section = createSection("introduction", "自我介绍", 0, intro);

        Map<String, Object> profile = new HashMap<>();
        profile.put("name", "张三");
        profile.put("email", "zhangsan@example.com");
        SectionDTO profileSection = createSection("profile", "个人信息", 1, profile);

        assertDoesNotThrow(() -> validator.validateForExport(List.of(profileSection, section)));
    }

    @Test
    void draft_shouldRejectInvalidSectionType() {
        SectionDTO section = createSection("unknown", "未知", 0, new HashMap<String, Object>());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> validator.validateDraft(List.of(section)));
        assertEquals(ResultCode.RESUME_SECTION_INVALID, ex.getErrorCode());
    }

    @Test
    void draft_shouldRejectTooManySections() {
        java.util.List<SectionDTO> sections = new java.util.ArrayList<>();
        for (int i = 0; i < 51; i++) {
            sections.add(createSection("custom", "模块" + i, i, new HashMap<String, Object>()));
        }
        BusinessException ex = assertThrows(BusinessException.class,
                () -> validator.validateDraft(sections));
        assertEquals(ResultCode.RESUME_CONTENT_TOO_LONG, ex.getErrorCode());
    }

    @Test
    void draft_shouldRejectContentTooLong() {
        Map<String, Object> data = new HashMap<>();
        // 单 Section 内字符数即可触发上限
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 210_000; i++) {
            sb.append('a');
        }
        data.put("content", sb.toString());
        SectionDTO section = createSection("custom", "大模块", 0, data);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> validator.validateDraft(List.of(section)));
        assertEquals(ResultCode.RESUME_CONTENT_TOO_LONG, ex.getErrorCode());
    }

    private SectionDTO createSection(String type, String title, int order, Object data) {
        SectionDTO section = new SectionDTO();
        section.setId("sec_" + type + "_" + order);
        section.setType(type);
        section.setTitle(title);
        section.setOrder(order);
        section.setVisible(true);
        section.setData(data);
        return section;
    }
}
