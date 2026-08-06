package com.resume.resume.controller;

import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.common.mapper.IdempotencyRecordMapper;
import com.resume.common.security.WithMockJwt;
import com.resume.common.service.RateLimiter;
import com.resume.resume.service.ResumeExportService;
import com.resume.user.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResumeExportController.class)
@AutoConfigureMockMvc(addFilters = false)
class ResumeExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResumeExportService resumeExportService;

    @MockBean
    private IdempotencyRecordMapper idempotencyRecordMapper;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private RateLimiter rateLimiter;

    @Test
    @WithMockJwt(userId = "user123")
    void exportMarkdown_shouldReturnMarkdownFile() throws Exception {
        when(resumeExportService.buildMarkdown(eq("user123"), eq("resume_1")))
                .thenReturn("# 张三\n\n> 目标岗位：Java 开发工程师\n");
        when(resumeExportService.buildExportFileName(eq("user123"), eq("resume_1"), eq("md")))
                .thenReturn("张三_Java开发工程师_简历.md");

        mockMvc.perform(get("/resumes/resume_1/export/markdown"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/markdown")))
                .andExpect(header().string("Content-Disposition",
                        containsString("filename*=UTF-8''%E5%BC%A0%E4%B8%89_Java%E5%BC%80%E5%8F%91%E5%B7%A5%E7%A8%8B%E5%B8%88_%E7%AE%80%E5%8E%86.md")))
                .andExpect(content().string(containsString("# 张三")));
    }

    @Test
    @WithMockJwt(userId = "user123")
    void exportWord_shouldReturnDocxBytes() throws Exception {
        byte[] docxBytes = "PK\u0003\u0004fake docx content".getBytes();
        when(resumeExportService.buildWord(eq("user123"), eq("resume_1")))
                .thenReturn(docxBytes);
        when(resumeExportService.buildExportFileName(eq("user123"), eq("resume_1"), eq("docx")))
                .thenReturn("张三_简历.docx");

        mockMvc.perform(get("/resumes/resume_1/export/word"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        containsString("application/vnd.openxmlformats-officedocument.wordprocessingml.document")))
                .andExpect(header().string("Content-Disposition", containsString("filename*=UTF-8''")))
                .andExpect(content().string(containsString("PK")));
    }

    @Test
    @WithMockJwt(userId = "user123")
    void exportMarkdown_missingName_shouldReturn400() throws Exception {
        when(resumeExportService.buildMarkdown(eq("user123"), eq("resume_1")))
                .thenThrow(new BusinessException(ResultCode.RESUME_PROFILE_NAME_REQUIRED,
                        "请先填写姓名，再进行导出。"));

        mockMvc.perform(get("/resumes/resume_1/export/markdown"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ResultCode.RESUME_PROFILE_NAME_REQUIRED));
    }

    @Test
    @WithMockJwt(userId = "user123")
    void exportWord_resumeNotFound_shouldReturn404() throws Exception {
        when(resumeExportService.buildWord(eq("user123"), eq("resume_missing")))
                .thenThrow(new BusinessException(ResultCode.RESUME_NOT_FOUND, "简历不存在。"));

        mockMvc.perform(get("/resumes/resume_missing/export/word"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ResultCode.RESUME_NOT_FOUND));
    }
}
