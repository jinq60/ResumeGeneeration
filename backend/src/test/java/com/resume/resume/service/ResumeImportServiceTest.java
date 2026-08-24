package com.resume.resume.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.common.constant.BizConstant;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.resume.dto.ResumeDetailResponse;
import com.resume.resume.dto.ResumeImportRequest;
import com.resume.resume.dto.SectionDTO;
import com.resume.resume.entity.Resume;
import com.resume.resume.mapper.ResumeMapper;
import com.resume.template.entity.Template;
import com.resume.template.service.TemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ResumeImportServiceTest {

    @Mock
    private ResumeMapper resumeMapper;

    @Mock
    private TemplateService templateService;

    @Mock
    private ResumeSectionValidator resumeSectionValidator;

    @Mock
    private com.resume.audit.service.ContentAuditService contentAuditService;

    private ResumeImportService service;

    @BeforeEach
    void setUp() {
        service = new ResumeImportService(resumeMapper, templateService,
                resumeSectionValidator, new ObjectMapper(), contentAuditService);
        Template template = new Template();
        template.setId("template_1");
        when(templateService.getTemplateEntity("template_1")).thenReturn(template);
    }

    private ResumeImportRequest buildRequest(String format, String content) {
        ResumeImportRequest request = new ResumeImportRequest();
        request.setTitle("导入的简历");
        request.setTemplateId("template_1");
        request.setFormat(format);
        request.setContent(content);
        return request;
    }

    @Test
    void parseMarkdown_shouldMapWorkSection() {
        String md = """
                # 张三
                ## 工作经历
                星云科技 · 产品经理
                - 负责企业协作产品规划
                - 提升活跃度 35%
                ## 自我介绍
                三年产品经验，专注企业服务。
                """;

        List<SectionDTO> sections = service.parseSections("markdown", md);

        assertEquals(2, sections.size());
        SectionDTO work = sections.get(0);
        assertEquals(BizConstant.SECTION_TYPE_WORK, work.getType());
        Map<?, ?> workItem = (Map<?, ?>) ((List<?>) work.getData()).get(0);
        assertEquals("星云科技 · 产品经理", workItem.get("company"));
        assertEquals(2, ((List<?>) workItem.get("description")).size());
        assertEquals(BizConstant.SECTION_TYPE_INTRODUCTION, sections.get(1).getType());
    }

    @Test
    void parseMarkdown_shouldMapUnknownSectionToCustom() {
        List<SectionDTO> sections = service.parseSections("markdown", """
                ## 获奖经历
                - 校一等奖学金
                """);

        assertEquals(1, sections.size());
        assertEquals(BizConstant.SECTION_TYPE_CUSTOM, sections.get(0).getType());
    }

    @Test
    void parseMarkdown_shouldMapSkillAndEducation() {
        List<SectionDTO> sections = service.parseSections("markdown", """
                ## 教育背景
                清华大学 · 计算机科学
                ## 技能
                - Java
                - Spring Boot
                """);

        assertEquals(2, sections.size());
        assertEquals(BizConstant.SECTION_TYPE_EDUCATION, sections.get(0).getType());
        assertEquals(BizConstant.SECTION_TYPE_SKILL, sections.get(1).getType());
    }

    @Test
    void parseJson_shouldAcceptSectionArray() {
        String json = """
                [{"id":"s1","type":"introduction","title":"自我介绍","order":0,"visible":true,
                  "data":{"content":"hello"}}]
                """;

        List<SectionDTO> sections = service.parseSections("json", json);

        assertEquals(1, sections.size());
        assertEquals("introduction", sections.get(0).getType());
    }

    @Test
    void parseJson_shouldAcceptSectionsObject() {
        String json = """
                {"sections":[{"id":"s1","type":"profile","title":"个人信息","order":0,"visible":true,
                  "data":{"name":"张三"}}]}
                """;

        List<SectionDTO> sections = service.parseSections("json", json);

        assertEquals(1, sections.size());
        assertEquals("profile", sections.get(0).getType());
    }

    @Test
    void parseJson_shouldRejectMalformedContent() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.parseSections("json", "{not valid json"));
        assertEquals(ResultCode.RESUME_IMPORT_INVALID, ex.getErrorCode());
    }

    @Test
    void importResume_shouldCreateResume() {
        when(resumeMapper.insert(any(Resume.class))).thenAnswer(inv -> {
            ((Resume) inv.getArgument(0)).setId("resume_1");
            return 1;
        });

        ResumeDetailResponse response = service.importResume("user_1",
                buildRequest("markdown", "## 自我介绍\n你好"));

        assertEquals("resume_1", response.getId());
        assertEquals("导入的简历", response.getTitle());
        assertEquals(1, response.getSections().size());
    }

    @Test
    void importResume_shouldRejectEmptyContent() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.importResume("user_1", buildRequest("markdown", "  ")));
        assertEquals(ResultCode.RESUME_IMPORT_INVALID, ex.getErrorCode());
    }

    @Test
    void importResume_shouldRejectUnknownTemplate() {
        when(templateService.getTemplateEntity("template_missing"))
                .thenThrow(new BusinessException(ResultCode.TEMPLATE_NOT_FOUND, "模板不存在。"));

        ResumeImportRequest request = buildRequest("markdown", "## 自我介绍\n你好");
        request.setTemplateId("template_missing");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.importResume("user_1", request));
        assertEquals(ResultCode.TEMPLATE_NOT_FOUND, ex.getErrorCode());
    }
}
