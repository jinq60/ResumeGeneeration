package com.resume.resume.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.common.entity.R;
import com.resume.common.security.WithMockJwt;
import com.resume.resume.dto.CreateResumeRequest;
import com.resume.resume.dto.PreviewResumeRequest;
import com.resume.resume.dto.ResumeDetailResponse;
import com.resume.resume.dto.UpdateResumeRequest;
import com.resume.resume.entity.Resume;
import com.resume.template.entity.Template;
import com.resume.template.mapper.TemplateMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 编辑器端到端测试：创建简历 -> 更新默认 Section -> 实时预览。
 * 使用 H2 内存数据库，验证前端编辑链路核心接口。
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
class ResumeEditorEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TemplateMapper templateMapper;

    private String templateId;

    @BeforeEach
    void setUp() {
        templateId = insertTestTemplate();
    }

    @Test
    @WithMockJwt(userId = "user_editor_1")
    void shouldCreateUpdateAndPreviewResume() throws Exception {
        // 1. 创建简历
        CreateResumeRequest createRequest = new CreateResumeRequest();
        createRequest.setTitle("编辑器测试简历");
        createRequest.setScene("campus_recruitment");
        createRequest.setTargetPosition("Java开发工程师");
        createRequest.setTemplateId(templateId);

        MvcResult createResult = mockMvc.perform(post("/resumes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        R<ResumeDetailResponse> created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        String resumeId = created.getData().getId();
        assertNotNull(resumeId);

        // 2. 更新/自动保存：携带前端初始化后的默认 Section
        UpdateResumeRequest updateRequest = new UpdateResumeRequest();
        updateRequest.setTitle(created.getData().getTitle());
        updateRequest.setTargetPosition("Java开发工程师");
        updateRequest.setSections(List.of(
                createSection("profile", "个人信息", 0, new HashMap<String, Object>()),
                createSection("education", "教育经历", 1, List.of()),
                createSection("project", "项目经历", 2, List.of()),
                createSection("work", "工作经历", 3, List.of()),
                createSection("skill", "技能 & 技术栈", 4, List.of()),
                createSection("introduction", "自我介绍", 5, new HashMap<String, Object>())
        ));

        mockMvc.perform(put("/resumes/{id}", resumeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 3. 实时预览：POST /resumes/preview
        Resume resume = new Resume();
        resume.setId(resumeId);
        resume.setUserId("user_editor_1");
        resume.setTitle(created.getData().getTitle());
        resume.setScene("campus_recruitment");
        resume.setTargetPosition("Java开发工程师");
        resume.setTemplateId(templateId);
        resume.setSections(updateRequest.getSections());

        PreviewResumeRequest previewRequest = new PreviewResumeRequest();
        previewRequest.setResume(resume);
        previewRequest.setTemplateId(templateId);

        mockMvc.perform(post("/resumes/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(previewRequest)))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String html = result.getResponse().getContentAsString();
                    assertNotNull(html);
                    assertEquals("text/html;charset=UTF-8", result.getResponse().getContentType());
                });
    }

    private String insertTestTemplate() {
        Template template = new Template();
        template.setCode("editor_test_tpl" + System.nanoTime());
        template.setName("编辑器测试模板");
        template.setCategory("classic");
        template.setConfig("{}");
        template.setHtmlTemplate("classic_single");
        template.setStatus("active");
        template.setIsBuiltin(0);
        template.setDeleted(0);
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());
        templateMapper.insert(template);
        return template.getId();
    }

    private com.resume.resume.dto.SectionDTO createSection(String type, String title, int order, Object data) {
        com.resume.resume.dto.SectionDTO section = new com.resume.resume.dto.SectionDTO();
        section.setId("sec_" + order);
        section.setType(type);
        section.setTitle(title);
        section.setOrder(order);
        section.setVisible(true);
        section.setData(data);
        return section;
    }
}
