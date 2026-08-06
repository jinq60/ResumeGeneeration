package com.resume.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.ai.dto.ResumeAiWriteRequest;
import com.resume.ai.dto.ResumeAiWriteResponse;
import com.resume.ai.service.AiWritingService;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.common.mapper.IdempotencyRecordMapper;
import com.resume.common.security.WithMockJwt;
import com.resume.common.service.RateLimiter;
import com.resume.user.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Flux;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(AiWritingController.class)
@AutoConfigureMockMvc(addFilters = false)
class AiWritingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiWritingService aiWritingService;

    @MockBean
    private IdempotencyRecordMapper idempotencyRecordMapper;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private RateLimiter rateLimiter;

    private ResumeAiWriteRequest buildRequest() {
        ResumeAiWriteRequest request = new ResumeAiWriteRequest();
        request.setSectionType("introduction");
        request.setField("content");
        request.setAction("polish");
        request.setOriginalText("自我介绍内容");
        return request;
    }

    @Test
    @WithMockJwt(userId = "user123")
    void write_shouldReturnContent() throws Exception {
        when(aiWritingService.write(eq("user123"), eq("resume123"), any(ResumeAiWriteRequest.class)))
                .thenReturn(new ResumeAiWriteResponse("优化后的内容"));

        mockMvc.perform(post("/resumes/resume123/ai/write")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").value("优化后的内容"));
    }

    @Test
    @WithMockJwt(userId = "user123")
    void write_shouldReturn400OnBusinessError() throws Exception {
        when(aiWritingService.write(eq("user123"), eq("resume123"), any(ResumeAiWriteRequest.class)))
                .thenThrow(new BusinessException(ResultCode.AI_WRITING_FIELD_INVALID, "该字段暂不支持 AI 写作。"));

        mockMvc.perform(post("/resumes/resume123/ai/write")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ResultCode.AI_WRITING_FIELD_INVALID));
    }

    @Test
    void write_shouldRejectMissingBodyField() throws Exception {
        mockMvc.perform(post("/resumes/resume123/ai/write")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sectionType\":\"introduction\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockJwt(userId = "user123")
    void stream_shouldReturnServerSentEvents() throws Exception {
        when(aiWritingService.stream(eq("user123"), eq("resume123"), any(ResumeAiWriteRequest.class)))
                .thenReturn(Flux.just("first", "second"));

        mockMvc.perform(post("/resumes/resume123/ai/write/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("event:delta")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("first")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("event:done")));
    }
}
