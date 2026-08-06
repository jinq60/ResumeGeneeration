package com.resume.resume.share.controller;

import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.common.mapper.IdempotencyRecordMapper;
import com.resume.common.security.WithMockJwt;
import com.resume.common.service.RateLimiter;
import com.resume.resume.share.dto.ShareResponse;
import com.resume.resume.share.service.ShareService;
import com.resume.user.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ShareController.class)
@AutoConfigureMockMvc(addFilters = false)
class ShareControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ShareService shareService;

    @MockBean
    private IdempotencyRecordMapper idempotencyRecordMapper;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private RateLimiter rateLimiter;

    private ShareResponse buildShareResponse() {
        ShareResponse response = new ShareResponse();
        response.setToken("share_token_1");
        response.setUrl("/share/share_token_1");
        response.setStatus("active");
        response.setCreatedAt(LocalDateTime.of(2026, 8, 5, 10, 0));
        return response;
    }

    @Test
    @WithMockJwt(userId = "user123")
    void createShare_shouldReturnTokenAndUrl() throws Exception {
        when(shareService.createShare(eq("user123"), eq("resume_1"), eq(false), eq(null)))
                .thenReturn(buildShareResponse());

        mockMvc.perform(post("/resumes/resume_1/share")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("share_token_1"))
                .andExpect(jsonPath("$.data.url").value("/share/share_token_1"));
    }

    @Test
    @WithMockJwt(userId = "user123")
    void createShare_withoutBody_shouldDefaultToVisibleContact() throws Exception {
        when(shareService.createShare(eq("user123"), eq("resume_1"), eq(false), eq(null)))
                .thenReturn(buildShareResponse());

        mockMvc.perform(post("/resumes/resume_1/share"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockJwt(userId = "user123")
    void createShare_withHideContact_shouldPassFlag() throws Exception {
        when(shareService.createShare(eq("user123"), eq("resume_1"), eq(true), eq(null)))
                .thenReturn(buildShareResponse());

        mockMvc.perform(post("/resumes/resume_1/share")
                        .contentType("application/json")
                        .content("{\"hideContact\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockJwt(userId = "user123")
    void createShare_withPastExpiresAt_shouldReturn400() throws Exception {
        mockMvc.perform(post("/resumes/resume_1/share")
                        .contentType("application/json")
                        .content("{\"expiresAt\":\"2020-01-01T00:00:00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ResultCode.PARAM_INVALID));
    }

    @Test
    @WithMockJwt(userId = "user123")
    void getShare_shouldReturnCurrentShare() throws Exception {
        when(shareService.getShare(eq("user123"), eq("resume_1")))
                .thenReturn(buildShareResponse());

        mockMvc.perform(get("/resumes/resume_1/share"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("active"));
    }

    @Test
    @WithMockJwt(userId = "user123")
    void getShare_withoutActiveShare_shouldReturnNullData() throws Exception {
        when(shareService.getShare(eq("user123"), eq("resume_1"))).thenReturn(null);

        mockMvc.perform(get("/resumes/resume_1/share"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    @WithMockJwt(userId = "user123")
    void revokeShare_shouldReturnSuccess() throws Exception {
        doNothing().when(shareService).revokeShare(eq("user123"), eq("resume_1"));

        mockMvc.perform(delete("/resumes/resume_1/share"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void renderSharePage_shouldReturnReadOnlyHtmlWithSecurityHeaders() throws Exception {
        when(shareService.renderSharePage("valid_token"))
                .thenReturn("<html><body><h1>张三的简历</h1></body></html>");

        mockMvc.perform(get("/share/valid_token"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Security-Policy",
                        "default-src 'none'; img-src 'self' data:; style-src 'unsafe-inline'; frame-ancestors 'none'"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("张三的简历")));
    }

    @Test
    void renderSharePage_invalidToken_shouldReturn404() throws Exception {
        when(shareService.renderSharePage("expired_token"))
                .thenThrow(new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "分享不存在或已失效。"));

        mockMvc.perform(get("/share/expired_token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ResultCode.RESOURCE_NOT_FOUND));
    }

    @Test
    @WithMockJwt(userId = "user123")
    void createShare_otherUsersResume_shouldReturn403() throws Exception {
        when(shareService.createShare(eq("user123"), any(), anyBoolean(), any()))
                .thenThrow(new BusinessException(ResultCode.ACCESS_DENIED, "无权访问该资源。"));

        mockMvc.perform(post("/resumes/resume_other/share")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResultCode.ACCESS_DENIED));
    }
}
