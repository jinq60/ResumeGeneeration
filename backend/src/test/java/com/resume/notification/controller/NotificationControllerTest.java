package com.resume.notification.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.resume.common.config.TestSecurityConfig;
import com.resume.notification.dto.NotificationResponse;
import com.resume.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 通知中心 controller 测试（v2.3 新接口）。
 */
@WebMvcTest(NotificationController.class)
@Import(TestSecurityConfig.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    private static org.springframework.test.web.servlet.request.RequestPostProcessor auth(String userId) {
        return authentication(new UsernamePasswordAuthenticationToken(userId, "N/A", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @Test
    void list_shouldReturnPagedNotifications() throws Exception {
        Page<NotificationResponse> page = new Page<>(1, 20);
        NotificationResponse n = new NotificationResponse();
        n.setId("notif_1");
        n.setType("pdf");
        n.setRead(false);
        page.setRecords(Collections.singletonList(n));
        page.setTotal(1);

        when(notificationService.list(eq("user_1"), anyInt(), anyInt(), anyBoolean())).thenReturn(page);

        mockMvc.perform(get("/notifications?unreadOnly=true").with(auth("user_1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].id").value("notif_1"));
    }

    @Test
    void unreadCount_shouldReturnMap() throws Exception {
        when(notificationService.unreadCount("user_1")).thenReturn(5L);

        mockMvc.perform(get("/notifications/unread-count").with(auth("user_1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(5));
    }

    @Test
    void markRead_shouldCallService() throws Exception {
        mockMvc.perform(put("/notifications/notif_1/read").with(csrf()).with(auth("user_1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(notificationService).markRead("user_1", "notif_1");
    }

    @Test
    void markAllRead_shouldCallService() throws Exception {
        mockMvc.perform(put("/notifications/read-all").with(csrf()).with(auth("user_1")))
                .andExpect(status().isOk());
        verify(notificationService).markAllRead("user_1");
    }

    @Test
    void delete_shouldCallService() throws Exception {
        mockMvc.perform(delete("/notifications/notif_1").with(csrf()).with(auth("user_1")))
                .andExpect(status().isOk());
        verify(notificationService).delete("user_1", "notif_1");
    }

    @Test
    void list_shouldRejectAnonymous() throws Exception {
        // SecurityConfig 要求 /notifications 需认证
        mockMvc.perform(get("/notifications"))
                .andExpect(status().is4xxClientError());
    }
}
