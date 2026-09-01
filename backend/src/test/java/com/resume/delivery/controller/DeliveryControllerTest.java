package com.resume.delivery.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.common.config.TestSecurityConfig;
import com.resume.delivery.dto.DeliveryRecordRequest;
import com.resume.delivery.dto.DeliveryRecordResponse;
import com.resume.delivery.service.DeliveryService;
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
 * 投递管理 controller 测试（v2.3 新接口）。
 */
@WebMvcTest(DeliveryController.class)
@Import(TestSecurityConfig.class)
class DeliveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeliveryService deliveryService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void create_shouldReturnRecord() throws Exception {
        DeliveryRecordRequest req = new DeliveryRecordRequest();
        req.setResumeId("resume_1");
        req.setCompany("Acme");
        req.setPosition("Backend Engineer");

        DeliveryRecordResponse resp = new DeliveryRecordResponse();
        resp.setId("del_1");
        resp.setCompany("Acme");
        when(deliveryService.create(any(), any(DeliveryRecordRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/deliveries")
                        .with(csrf())
                        .with(user("user_1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("del_1"));
    }

    @Test
    void list_shouldPassFilters() throws Exception {
        Page<DeliveryRecordResponse> page = new Page<>(1, 20);
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        when(deliveryService.list(eq("user_1"), anyInt(), anyInt(),
                eq("java"), eq("Acme"), eq("Backend"), eq("delivered"),
                any(), any())).thenReturn(page);

        mockMvc.perform(get("/deliveries?keyword=java&company=Acme&position=Backend&status=delivered")
                        .with(user("user_1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void get_shouldReturnRecord() throws Exception {
        DeliveryRecordResponse resp = new DeliveryRecordResponse();
        resp.setId("del_1");
        when(deliveryService.get(any(), eq("del_1"))).thenReturn(resp);

        mockMvc.perform(get("/deliveries/del_1").with(user("user_1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("del_1"));
    }

    @Test
    void delete_shouldCallService() throws Exception {
        mockMvc.perform(delete("/deliveries/del_1").with(csrf()).with(user("user_1")))
                .andExpect(status().isOk());
        verify(deliveryService).delete(any(), eq("del_1"));
    }

    @Test
    void create_shouldRejectAnonymous() throws Exception {
        mockMvc.perform(post("/deliveries")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is4xxClientError());
    }
}
