package com.resume.resume.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.common.entity.R;
import com.resume.resume.dto.CreateResumeRequest;
import com.resume.resume.dto.ResumeDetailResponse;
import com.resume.resume.dto.ResumeListItemResponse;
import com.resume.resume.dto.UpdateResumeRequest;
import com.resume.resume.service.ResumeService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Disabled("需要修复Spring Security配置问题")
@WebMvcTest(ResumeController.class)
class ResumeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ResumeService resumeService;

    @Test
    @WithMockUser(username = "user123")
    void testCreateResume_Success() throws Exception {
        CreateResumeRequest request = new CreateResumeRequest();
        request.setTitle("我的简历");
        request.setScene("campus_recruitment");
        request.setTargetPosition("Java开发工程师");

        ResumeDetailResponse response = new ResumeDetailResponse();
        response.setId("resume123");
        response.setTitle("我的简历");

        when(resumeService.createResume(eq("user123"), any(CreateResumeRequest.class))).thenReturn(response);

        mockMvc.perform(post("/resumes")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("resume123"))
                .andExpect(jsonPath("$.data.title").value("我的简历"));
    }

    @Test
    @WithMockUser(username = "user123")
    void testListResumes_Success() throws Exception {
        Page<ResumeListItemResponse> page = new Page<>(1, 20);
        ResumeListItemResponse item = new ResumeListItemResponse();
        item.setId("resume123");
        item.setTitle("我的简历");
        page.setRecords(Collections.singletonList(item));

        when(resumeService.listResumes(eq("user123"), eq(1), eq(20))).thenReturn(page);

        mockMvc.perform(get("/resumes")
                .with(csrf())
                .param("page", "1")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].id").value("resume123"));
    }

    @Test
    @WithMockUser(username = "user123")
    void testGetResume_Success() throws Exception {
        ResumeDetailResponse response = new ResumeDetailResponse();
        response.setId("resume123");
        response.setTitle("我的简历");

        when(resumeService.getResume(eq("user123"), eq("resume123"))).thenReturn(response);

        mockMvc.perform(get("/resumes/resume123")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("resume123"));
    }

    @Test
    @WithMockUser(username = "user123")
    void testUpdateResume_Success() throws Exception {
        UpdateResumeRequest request = new UpdateResumeRequest();
        request.setTitle("更新后的简历");

        when(resumeService.updateResume(eq("user123"), eq("resume123"), any(UpdateResumeRequest.class)))
                .thenReturn(Collections.singletonMap("id", "resume123"));

        mockMvc.perform(put("/resumes/resume123")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("resume123"));
    }

    @Test
    @WithMockUser(username = "user123")
    void testDeleteResume_Success() throws Exception {
        mockMvc.perform(delete("/resumes/resume123")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
