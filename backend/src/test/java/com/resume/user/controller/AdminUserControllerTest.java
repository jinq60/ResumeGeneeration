package com.resume.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.resume.common.config.TestSecurityConfig;
import com.resume.user.dto.AdminUserDetailResponse;
import com.resume.user.dto.AdminUserListItemResponse;
import com.resume.user.dto.ResetPasswordResponse;
import com.resume.user.service.AdminUserService;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminUserController.class)
@Import(TestSecurityConfig.class)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminUserService adminUserService;

    @Test
    void list_shouldReturnUsers_whenAdmin() throws Exception {
        Page<AdminUserListItemResponse> page = new Page<>(1, 20);
        AdminUserListItemResponse item = new AdminUserListItemResponse();
        item.setUserId("user1");
        item.setNickname("Alice");
        item.setStatus("active");
        page.setRecords(Collections.singletonList(item));
        page.setTotal(1);

        when(adminUserService.listUsers(anyInt(), anyInt(), eq(null), eq(null))).thenReturn(page);

        mockMvc.perform(get("/admin/users")
                .with(csrf())
                .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].userId").value("user1"));
    }

    @Test
    void list_shouldRejectNonAdmin() throws Exception {
        mockMvc.perform(get("/admin/users")
                .with(csrf())
                .with(user("normal").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void get_shouldReturnUserDetail() throws Exception {
        AdminUserDetailResponse response = new AdminUserDetailResponse();
        response.setUserId("user1");
        response.setNickname("Alice");
        response.setRole("USER");
        response.setStatus("active");

        when(adminUserService.getUser("user1")).thenReturn(response);

        mockMvc.perform(get("/admin/users/user1")
                .with(csrf())
                .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value("user1"));
    }

    @Test
    void updateStatus_shouldSucceed() throws Exception {
        mockMvc.perform(patch("/admin/users/user1/status")
                .with(csrf())
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"disabled\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void resetPassword_shouldReturnTemporaryPassword() throws Exception {
        ResetPasswordResponse response = new ResetPasswordResponse();
        response.setUserId("user1");
        response.setTemporaryPassword("abc123XYZ");

        when(adminUserService.resetPassword(eq("user1"), any())).thenReturn(response);

        mockMvc.perform(post("/admin/users/user1/reset-password")
                .with(csrf())
                .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value("user1"))
                .andExpect(jsonPath("$.data.temporaryPassword").exists());
    }

    @Test
    void delete_shouldSucceed() throws Exception {
        mockMvc.perform(delete("/admin/users/user1")
                .with(csrf())
                .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}