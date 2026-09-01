package com.resume.audit.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.resume.audit.dto.AuditItemResponse;
import com.resume.audit.dto.AuditStatsResponse;
import com.resume.audit.service.ContentAuditService;
import com.resume.common.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 管理端内容审核 controller 测试（v2.3 新接口）。
 */
@WebMvcTest(AdminAuditController.class)
@Import(TestSecurityConfig.class)
class AdminAuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ContentAuditService contentAuditService;

    @Test
    void list_shouldReturnPagedAudits_whenAdmin() throws Exception {
        Page<AuditItemResponse> page = new Page<>(1, 20);
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        when(contentAuditService.list(anyInt(), anyInt(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/admin/audits?status=pending&riskLevel=high")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void stats_shouldReturnAggregations() throws Exception {
        AuditStatsResponse stats = new AuditStatsResponse();
        stats.setTotal(100L);
        when(contentAuditService.stats()).thenReturn(stats);

        mockMvc.perform(get("/admin/audits/stats").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(100));
    }

    @Test
    void approve_shouldCallService() throws Exception {
        AuditItemResponse resp = new AuditItemResponse();
        resp.setId("audit_1");
        resp.setStatus("approved");
        when(contentAuditService.review(any(), eq("audit_1"), eq("approve"),
                any(), any())).thenReturn(resp);

        mockMvc.perform(post("/admin/audits/audit_1/approve")
                        .with(csrf())
                        .with(user("admin_1").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"ok\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("approved"));
        verify(contentAuditService).review(any(), eq("audit_1"), eq("approve"),
                any(), any());
    }

    @Test
    void reject_shouldCallService() throws Exception {
        AuditItemResponse resp = new AuditItemResponse();
        resp.setStatus("rejected");
        when(contentAuditService.review(eq("admin_1"), eq("audit_1"), eq("reject"),
                any(), eq(null))).thenReturn(resp);

        mockMvc.perform(post("/admin/audits/audit_1/reject")
                        .with(csrf())
                        .with(user("admin_1").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"违规\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void markWarning_shouldPassRiskLevel() throws Exception {
        AuditItemResponse resp = new AuditItemResponse();
        when(contentAuditService.review(any(), eq("audit_1"), eq("warning"),
                any(), eq("medium"))).thenReturn(resp);

        mockMvc.perform(post("/admin/audits/audit_1/mark-warning")
                        .with(csrf())
                        .with(user("admin_1").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"注意\",\"riskLevel\":\"medium\"}"))
                .andExpect(status().isOk());
        verify(contentAuditService).review(any(), eq("audit_1"), eq("warning"),
                any(), eq("medium"));
    }
}
