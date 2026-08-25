package com.resume.resume.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.common.mapper.IdempotencyRecordMapper;
import com.resume.common.security.WithMockJwt;
import com.resume.common.service.RateLimiter;
import com.resume.common.service.ResumeRenderService;
import com.resume.resume.dto.PreviewResumeRequest;
import com.resume.resume.entity.Resume;
import com.resume.resume.service.ResumeSectionValidator;
import com.resume.resume.service.ResumeService;
import com.resume.template.entity.Template;
import com.resume.template.service.TemplateService;
import com.resume.user.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PreviewController.class)
@AutoConfigureMockMvc(addFilters = false)
class PreviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ResumeService resumeService;

    @MockBean
    private TemplateService templateService;

    @MockBean
    private ResumeRenderService resumeRenderService;

    @MockBean
    private ResumeSectionValidator resumeSectionValidator;

    @MockBean
    private IdempotencyRecordMapper idempotencyRecordMapper;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private RateLimiter rateLimiter;

    @Test
    @WithMockJwt(userId = "user123")
    void testPreviewLive_Success() throws Exception {
        Resume resume = new Resume();
        resume.setId("resume123");
        resume.setTemplateId("template_classic_single");
        resume.setSections(List.of());

        Template template = new Template();
        template.setId("template_classic_single");
        template.setCode("template_classic_single");
        template.setConfig("{}");
        template.setHtmlTemplate("classic_single");

        PreviewResumeRequest request = new PreviewResumeRequest();
        request.setResume(resume);
        request.setTemplateId("template_classic_single");

        when(templateService.getTemplateEntityForRender(eq("template_classic_single"))).thenReturn(template);
        when(resumeRenderService.render(eq(resume), eq(template))).thenReturn("<html>preview</html>");

        mockMvc.perform(post("/resumes/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("<html>preview</html>"));
    }

    @Test
    void testPreviewLive_MissingResume() throws Exception {
        PreviewResumeRequest request = new PreviewResumeRequest();
        request.setResume(null);

        mockMvc.perform(post("/resumes/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"code\":400,\"message\":\"简历数据不能为空。\"}"));
    }
}
