package com.resume.user.controller;

import com.resume.common.config.TestSecurityConfig;
import com.resume.user.dto.UserInfoResponse;
import com.resume.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(TestSecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void me_shouldReturnCurrentUser() throws Exception {
        UserInfoResponse response = new UserInfoResponse();
        response.setUserId("user123");
        response.setNickname("张三");
        response.setPhone("138****8000");
        response.setEmail("z***@example.com");
        response.setIsGuest(false);

        when(userService.getCurrentUser(any())).thenReturn(response);

        mockMvc.perform(get("/users/me")
                .with(csrf())
                .with(user("user123").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value("user123"))
                .andExpect(jsonPath("$.data.nickname").value("张三"))
                .andExpect(jsonPath("$.data.isGuest").value(false));
    }

    @Test
    void me_shouldRejectUnauthenticated() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isForbidden());
    }
}