package com.resume.resume.service;

import com.resume.common.constant.BizConstant;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.resume.dto.SectionDTO;
import com.resume.resume.entity.Resume;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ResumeExportServiceTest {

    @Mock
    private ResumeService resumeService;

    private ResumeExportService exportService;

    @BeforeEach
    void setUp() {
        exportService = new ResumeExportService(resumeService, new ResumeSectionValidator());
    }

    private Resume buildResume() {
        Resume resume = new Resume();
        resume.setId("resume_1");
        resume.setUserId("user_1");
        resume.setTitle("我的简历");
        resume.setTargetPosition("Java 后端开发");
        resume.setDeleted(BizConstant.NOT_DELETED);

        Map<String, Object> profile = new HashMap<>();
        profile.put("name", "张三");
        profile.put("phone", "13800000000");
        profile.put("email", "zhangsan@example.com");
        profile.put("city", "北京");
        resume.setSections(List.of(
                section("profile", "个人信息", 0, profile, true),
                section("introduction", "自我介绍", 1, Map.of("content", "热爱技术，乐于钻研。"), true),
                section("work", "工作经历", 2, List.of(
                        Map.of("company", "星云科技", "position", "后端工程师",
                                "description", List.of("负责订单系统开发", "性能优化 30%"))
                ), true),
                section("project", "项目经历", 3, List.of(
                        Map.of("name", "简历生成工具", "role", "全栈",
                                "description", List.of("实现在线编辑与导出"))
                ), false)
        ));
        return resume;
    }

    private SectionDTO section(String type, String title, int order, Object data, boolean visible) {
        SectionDTO section = new SectionDTO();
        section.setId("sec_" + type);
        section.setType(type);
        section.setTitle(title);
        section.setOrder(order);
        section.setVisible(visible);
        section.setData(data);
        return section;
    }

    @Test
    void buildMarkdown_shouldContainKeySectionsAndSkipHidden() {
        when(resumeService.getResumeEntity("user_1", "resume_1")).thenReturn(buildResume());

        String markdown = exportService.buildMarkdown("user_1", "resume_1");

        assertTrue(markdown.contains("# 张三"));
        assertTrue(markdown.contains("13800000000"));
        assertTrue(markdown.contains("## 自我介绍"));
        assertTrue(markdown.contains("热爱技术"));
        assertTrue(markdown.contains("星云科技"));
        assertTrue(markdown.contains("性能优化 30%"));
        // hidden project section skipped
        assertFalse(markdown.contains("简历生成工具"));
    }

    @Test
    void buildMarkdown_shouldRejectMissingName() {
        Resume resume = buildResume();
        Map<String, Object> profile = new HashMap<>();
        profile.put("phone", "13800000000");
        resume.setSections(List.of(section("profile", "个人信息", 0, profile, true)));
        when(resumeService.getResumeEntity("user_1", "resume_1")).thenReturn(resume);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> exportService.buildMarkdown("user_1", "resume_1"));
        assertEquals(ResultCode.PDF_EXPORT_NAME_REQUIRED, ex.getErrorCode());
    }

    @Test
    void buildMarkdown_shouldReadRichTextAndDropUnsafeMarkup() {
        Resume resume = buildResume();
        Map<String, Object> intro = new HashMap<>();
        intro.put("content", "纯文本");
        intro.put("contentHtml", "<p><strong>富文本介绍</strong></p><script>alert(1)</script>");
        resume.getSections().get(1).setData(intro);
        when(resumeService.getResumeEntity("user_1", "resume_1")).thenReturn(resume);

        String markdown = exportService.buildMarkdown("user_1", "resume_1");

        assertTrue(markdown.contains("富文本介绍"));
        assertFalse(markdown.contains("alert(1)"));
    }

    @Test
    void buildWord_shouldReturnValidDocxBytes() {
        when(resumeService.getResumeEntity("user_1", "resume_1")).thenReturn(buildResume());

        byte[] bytes = exportService.buildWord("user_1", "resume_1");

        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
        // docx 为 zip 格式，文件头为 PK
        assertEquals('P', bytes[0]);
        assertEquals('K', bytes[1]);
    }

    @Test
    void buildExportFileName_shouldUseNameAndPosition() {
        when(resumeService.getResumeEntity("user_1", "resume_1")).thenReturn(buildResume());

        assertEquals("张三_Java 后端开发_简历.docx",
                exportService.buildExportFileName("user_1", "resume_1", "docx"));
        assertEquals("张三_Java 后端开发_简历.md",
                exportService.buildExportFileName("user_1", "resume_1", "md"));
    }
}
