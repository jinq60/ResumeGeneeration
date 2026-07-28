package com.resume.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.common.config.TestSecurityConfig;
import com.resume.user.dto.AuthResponse;
import com.resume.user.dto.LoginRequest;
import com.resume.user.dto.RegisterRequest;
import com.resume.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    void testRegister_Success() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setPhone("13800138000");
        request.setVerifyCode("123456");
        request.setPassword("password123");

        AuthResponse response = new AuthResponse();
        response.setUserId("user123");
        response.setAccessToken("token123");
        response.setIsGuest(false);

        when(userService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value("user123"))
                .andExpect(jsonPath("$.data.accessToken").value("token123"));
    }

    @Test
    void testRegister_InvalidPhone() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setPhone("invalid");
        request.setVerifyCode("123456");
        request.setPassword("password123");

        mockMvc.perform(post("/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testLogin_Success() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setAccount("13800138000");
        request.setPassword("password123");
        request.setLoginType("phone");

        AuthResponse response = new AuthResponse();
        response.setUserId("user123");
        response.setAccessToken("token123");
        response.setIsGuest(false);

        when(userService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value("user123"))
                .andExpect(jsonPath("$.data.accessToken").value("token123"));
    }

    @Test
    void testGuest_Success() throws Exception {
        AuthResponse response = new AuthResponse();
        response.setUserId("guest123");
        response.setAccessToken("guestToken123");
        response.setIsGuest(true);

        when(userService.createGuest()).thenReturn(response);

        mockMvc.perform(post("/auth/guest")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value("guest123"))
                .andExpect(jsonPath("$.data.isGuest").value(true));
    }
}
