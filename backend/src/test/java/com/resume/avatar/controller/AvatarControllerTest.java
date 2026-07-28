package com.resume.avatar.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.avatar.dto.AvatarUploadResponse;
import com.resume.avatar.dto.OptimizeAvatarRequest;
import com.resume.avatar.service.AvatarService;
import com.resume.common.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AvatarController.class)
@Import(TestSecurityConfig.class)
class AvatarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AvatarService avatarService;

    @Test
    void upload_shouldReturnUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1, 2, 3});

        AvatarUploadResponse response = new AvatarUploadResponse();
        response.setId("avatar_1");
        response.setSourceImageUrl("/uploads/avatars/u1/avatars/x.png");
        response.setFileName("avatar.png");

        when(avatarService.uploadAvatar(any(), any(), eq("resume_1"))).thenReturn(response);

        mockMvc.perform(multipart("/avatars/upload")
                .file(file)
                .param("resumeId", "resume_1")
                .with(csrf())
                .with(user("user123").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("avatar_1"));
    }

    @Test
    void optimize_shouldReturnTaskId() throws Exception {
        OptimizeAvatarRequest request = new OptimizeAvatarRequest();
        request.setSourceImageUrl("/uploads/avatars/u1/src.png");
        request.setBackgroundType("white");
        request.setStyle("formal");

        when(avatarService.optimizeAvatar(any(), any()))
                .thenReturn(Map.of("taskId", "avatar_1", "status", "success"));

        mockMvc.perform(post("/avatars/optimize")
                .with(csrf())
                .with(user("user123").roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").value("avatar_1"));
    }

    @Test
    void optimize_shouldRejectInvalidBackground() throws Exception {
        OptimizeAvatarRequest request = new OptimizeAvatarRequest();
        request.setSourceImageUrl("/uploads/avatars/u1/src.png");
        request.setBackgroundType("pink"); // invalid
        request.setStyle("formal");

        mockMvc.perform(post("/avatars/optimize")
                .with(csrf())
                .with(user("user123").roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}