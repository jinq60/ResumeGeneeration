package com.resume.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.ai.dto.ResumeOptimizeRequest;
import com.resume.ai.dto.ResumeOptimizeResponse;
import com.resume.ai.service.AiResumeOptimizeService;
import com.resume.common.config.TestSecurityConfig;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.resume.entity.Resume;
import com.resume.resume.service.ResumeService;
import com.resume.ai.mapper.ResumeOptimizeTaskMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AiResumeController.class)
@Import(TestSecurityConfig.class)
class AiResumeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ResumeService resumeService;

    @MockBean
    private ResumeOptimizeTaskMapper optimizeTaskMapper;

    @MockBean
    private AiResumeOptimizeService aiResumeOptimizeService;

    @Test
    void createOptimize_shouldReturnPendingTask() throws Exception {
        Resume resume = new Resume();
        resume.setId("resume_1");
        resume.setUserId("user123");

        when(resumeService.getResumeEntity(any(), eq("resume_1"))).thenReturn(resume);
        when(optimizeTaskMapper.selectCount(any())).thenReturn(0L);
        doNothing().when(aiResumeOptimizeService)
                .executeOptimize(anyString(), any(Resume.class), anyString());

        ResumeOptimizeRequest request = new ResumeOptimizeRequest();
        request.setJobDescription("Java 后端工程师，3 年经验");

        mockMvc.perform(post("/resumes/resume_1/optimize")
                .with(csrf())
                .with(user("user123").roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resumeId").value("resume_1"))
                .andExpect(jsonPath("$.data.status").value("pending"));
    }

    @Test
    void createOptimize_shouldRejectConcurrentLimit() throws Exception {
        Resume resume = new Resume();
        resume.setId("resume_1");
        resume.setUserId("user123");

        when(resumeService.getResumeEntity(any(), eq("resume_1"))).thenReturn(resume);
        when(optimizeTaskMapper.selectCount(any())).thenReturn(3L);

        ResumeOptimizeRequest request = new ResumeOptimizeRequest();
        request.setJobDescription("Java 后端工程师");

        mockMvc.perform(post("/resumes/resume_1/optimize")
                .with(csrf())
                .with(user("user123").roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOptimize_shouldRejectEmptyJobDescription() throws Exception {
        ResumeOptimizeRequest request = new ResumeOptimizeRequest();
        request.setJobDescription("");

        mockMvc.perform(post("/resumes/resume_1/optimize")
                .with(csrf())
                .with(user("user123").roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOptimizeResult_shouldRejectOthersTask() throws Exception {
        when(optimizeTaskMapper.selectById("task_1"))
                .thenThrow(new BusinessException(ResultCode.ACCESS_DENIED, "无权访问该资源。"));

        mockMvc.perform(get("/resumes/resume_1/optimize/task_1")
                .with(csrf())
                .with(user("user123").roles("USER")))
                .andExpect(status().isForbidden());
    }
}