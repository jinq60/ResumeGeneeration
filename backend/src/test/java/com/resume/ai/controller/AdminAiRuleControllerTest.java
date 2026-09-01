package com.resume.ai.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.resume.ai.dto.AiRuleResponse;
import com.resume.ai.service.AiRuleService;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AI 规则管理 controller 测试（v2.3 新接口）。
 */
@WebMvcTest(AdminAiRuleController.class)
@Import(TestSecurityConfig.class)
class AdminAiRuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiRuleService aiRuleService;

    @Test
    void list_shouldReturnPagedRules_whenAdmin() throws Exception {
        Page<AiRuleResponse> page = new Page<>(1, 20);
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        when(aiRuleService.list(anyInt(), anyInt(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/admin/ai-rules?ruleType=writing&status=active")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void stats_shouldReturnAggregations() throws Exception {
        com.resume.ai.dto.AiRuleStatsResponse stats = new com.resume.ai.dto.AiRuleStatsResponse();
        stats.setTotal(5);
        when(aiRuleService.stats()).thenReturn(stats);

        mockMvc.perform(get("/admin/ai-rules/stats").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void create_shouldReturnNewRule() throws Exception {
        AiRuleResponse resp = new AiRuleResponse();
        resp.setId("rule_1");
        resp.setVersion(1);
        when(aiRuleService.create(any(), any())).thenReturn(resp);

        mockMvc.perform(post("/admin/ai-rules")
                        .with(csrf())
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"写作规则\",\"ruleType\":\"writing\",\"content\":\"{}\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("rule_1"));
    }

    @Test
    void publish_shouldCallService() throws Exception {
        AiRuleResponse resp = new AiRuleResponse();
        resp.setId("rule_1");
        resp.setVersion(2);
        when(aiRuleService.publish(any(), any(), any())).thenReturn(resp);

        mockMvc.perform(post("/admin/ai-rules/rule_1/publish")
                        .with(csrf())
                        .with(user("admin_1").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(2));
        verify(aiRuleService).publish(any(), any(), any());
    }

    @Test
    void delete_shouldCallService() throws Exception {
        mockMvc.perform(delete("/admin/ai-rules/rule_1")
                        .with(csrf())
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        verify(aiRuleService).delete("rule_1");
    }
}
