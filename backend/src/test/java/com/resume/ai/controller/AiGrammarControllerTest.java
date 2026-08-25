package com.resume.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.ai.dto.GrammarCheckResponse;
import com.resume.ai.service.AiGrammarService;
import com.resume.common.mapper.IdempotencyRecordMapper;
import com.resume.common.security.WithMockJwt;
import com.resume.common.service.RateLimiter;
import com.resume.user.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiGrammarController.class)
@AutoConfigureMockMvc(addFilters = false)
class AiGrammarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiGrammarService aiGrammarService;
    @MockBean
    private IdempotencyRecordMapper idempotencyRecordMapper;
    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    @MockBean
    private RateLimiter rateLimiter;

    @Test
    @WithMockJwt(userId = "user123")
    void check_shouldReturnIssues() throws Exception {
        GrammarCheckResponse response = new GrammarCheckResponse();
        response.setStatus("success");
        response.setModel("qwen-turbo");
        GrammarCheckResponse.GrammarIssue issue = new GrammarCheckResponse.GrammarIssue();
        issue.setSectionType("work");
        issue.setField("description");
        issue.setSeverity("high");
        issue.setOriginalText("负责系统开发");
        issue.setSuggestion("主导订单系统架构设计与开发");
        issue.setExplanation("建议补充成果和范围。");
        response.setIssues(List.of(issue));
        // @WithMockJwt 的 credentials 为字符串 token，getGuest mock 返回 false → 非游客
        when(aiGrammarService.check(eq("user123"), anyBoolean(), eq("resume123"))).thenReturn(response);

        mockMvc.perform(post("/resumes/resume123/grammar-check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("success"))
                .andExpect(jsonPath("$.data.issues[0].field").value("description"));
    }
}
