package com.resume.resume.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.common.config.TestSecurityConfig;
import com.resume.resume.dto.CreateResumeRequest;
import com.resume.resume.dto.ResumeDetailResponse;
import com.resume.resume.dto.ResumeListItemResponse;
import com.resume.resume.dto.UpdateResumeRequest;
import com.resume.resume.dto.UpdateResumeResponse;
import com.resume.resume.service.ResumeReviewService;
import com.resume.resume.service.ResumeService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ResumeController.class)
@Import(TestSecurityConfig.class)
class ResumeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ResumeService resumeService;

    @MockBean
    private ResumeReviewService resumeReviewService;

    private static final String USER_ID = "user123";

    @Test
    void testCreateResume_Success() throws Exception {
        CreateResumeRequest request = new CreateResumeRequest();
        request.setTitle("我的简历");
        request.setScene("campus_recruitment");
        request.setTargetPosition("Java开发工程师");
        request.setTemplateId("template_1");

        ResumeDetailResponse response = new ResumeDetailResponse();
        response.setId("resume123");
        response.setTitle("我的简历");

        when(resumeService.createResume(any(), any(CreateResumeRequest.class))).thenReturn(response);

        mockMvc.perform(post("/resumes")
                .with(csrf())
                .with(user(USER_ID).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("resume123"))
                .andExpect(jsonPath("$.data.title").value("我的简历"));
    }

    @Test
    void testListResumes_Success() throws Exception {
        Page<ResumeListItemResponse> page = new Page<>(1, 20);
        ResumeListItemResponse item = new ResumeListItemResponse();
        item.setId("resume123");
        item.setTitle("我的简历");
        page.setRecords(Collections.singletonList(item));
        page.setTotal(1);
        page.setCurrent(1);

        when(resumeService.listResumes(any(), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/resumes")
                .with(csrf())
                .with(user(USER_ID).roles("USER"))
                .param("page", "1")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].id").value("resume123"));
    }

    @Test
    void testGetResume_Success() throws Exception {
        ResumeDetailResponse response = new ResumeDetailResponse();
        response.setId("resume123");
        response.setTitle("我的简历");

        when(resumeService.getResume(any(), any())).thenReturn(response);

        mockMvc.perform(get("/resumes/resume123")
                .with(csrf())
                .with(user(USER_ID).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("resume123"));
    }

    @Test
    void testUpdateResume_Success() throws Exception {
        UpdateResumeRequest request = new UpdateResumeRequest();
        request.setTitle("更新后的简历");

        when(resumeService.updateResume(any(), any(), any(UpdateResumeRequest.class)))
                .thenReturn(buildUpdateResponse());

        mockMvc.perform(put("/resumes/resume123")
                .with(csrf())
                .with(user(USER_ID).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("resume123"));
    }

    @Test
    void testDeleteResume_Success() throws Exception {
        mockMvc.perform(delete("/resumes/resume123")
                .with(csrf())
                .with(user(USER_ID).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    private UpdateResumeResponse buildUpdateResponse() {
        UpdateResumeResponse response = new UpdateResumeResponse();
        response.setId("resume123");
        response.setUpdatedAt(LocalDateTime.now());
        return response;
    }
}
