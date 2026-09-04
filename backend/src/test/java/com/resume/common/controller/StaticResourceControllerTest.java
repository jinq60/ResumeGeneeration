package com.resume.common.controller;

import com.resume.common.service.MinioStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 验证 /uploads/** 静态资源在 context-path=/api 下的路由与安全放行：
 * 浏览器访问 /uploads/... 由网关（vite/nginx）改写为 /api/uploads/...，
 * 容器剥离 context-path 后由 StaticResourceController 提供服务。
 * MockMvc 请求通过 {@code contextPath("/api")} 模拟容器行为。
 */
@SpringBootTest(properties = "server.servlet.context-path=/api")
@AutoConfigureMockMvc
class StaticResourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MinioStorageService minioStorageService;

    @MockBean
    private com.resume.resume.share.mapper.ResumeShareMapper resumeShareMapper;

    @Test
    @com.resume.common.security.WithMockJwt(userId = "user_1")
    void shouldServeUploadsUnderContextPath() throws Exception {
        when(minioStorageService.getBucketAvatars()).thenReturn("resume-avatars");
        when(minioStorageService.downloadStream("resume-avatars", "user_1/avatars/a.png"))
                .thenReturn(new java.io.ByteArrayInputStream(new byte[]{1, 2, 3}));

        // 已登录可直接访问头像私有资源
        mockMvc.perform(get("/api/uploads/avatars/user_1/avatars/a.png").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    void shouldRejectPathTraversal() throws Exception {
        when(minioStorageService.getBucketAvatars()).thenReturn("resume-avatars");

        mockMvc.perform(get("/api/uploads/avatars/../secret.png").contextPath("/api"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectForgedReferer() throws Exception {
        when(minioStorageService.getBucketAvatars()).thenReturn("resume-avatars");
        when(minioStorageService.downloadStream("resume-avatars", "u/a.png"))
                .thenReturn(new java.io.ByteArrayInputStream(new byte[]{1}));

        // 伪造 Referer 不应再被放行，统一 404 避免匿名枚举
        mockMvc.perform(get("/api/uploads/avatars/u/a.png").contextPath("/api")
                        .header("Referer", "https://evil.example/share/fake"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectDoubleEncodedTraversal() throws Exception {
        when(minioStorageService.getBucketAvatars()).thenReturn("resume-avatars");

        mockMvc.perform(get("/api/uploads/avatars/%252e%252e/secret.png").contextPath("/api"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404ForAnonymousWithoutToken() throws Exception {
        when(minioStorageService.getBucketAvatars()).thenReturn("resume-avatars");

        // 无凭证匿名访问统一 404（消除 401/404 预言机）
        mockMvc.perform(get("/api/uploads/avatars/u/a.png").contextPath("/api"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectCrossUserShareToken() throws Exception {
        when(minioStorageService.getBucketAvatars()).thenReturn("resume-avatars");
        when(minioStorageService.downloadStream("resume-avatars", "user_2/avatars/a.png"))
                .thenReturn(new java.io.ByteArrayInputStream(new byte[]{1}));
        com.resume.resume.share.entity.ResumeShare share = new com.resume.resume.share.entity.ResumeShare();
        share.setUserId("user_1");
        share.setResumeId("resume_1");
        share.setToken("tok-user1");
        share.setStatus("active");
        share.setDeleted(0);
        when(resumeShareMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(share);

        // user_1 的分享 token 不得读取 user_2 的头像
        mockMvc.perform(get("/api/uploads/avatars/user_2/avatars/a.png").contextPath("/api")
                        .param("shareToken", "tok-user1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldAllowSameUserShareToken() throws Exception {
        when(minioStorageService.getBucketAvatars()).thenReturn("resume-avatars");
        when(minioStorageService.downloadStream("resume-avatars", "user_1/avatars/a.png"))
                .thenReturn(new java.io.ByteArrayInputStream(new byte[]{1, 2, 3}));
        com.resume.resume.share.entity.ResumeShare share = new com.resume.resume.share.entity.ResumeShare();
        share.setUserId("user_1");
        share.setResumeId("resume_1");
        share.setToken("tok-user1");
        share.setStatus("active");
        share.setDeleted(0);
        when(resumeShareMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(share);

        mockMvc.perform(get("/api/uploads/avatars/user_1/avatars/a.png").contextPath("/api")
                        .param("shareToken", "tok-user1"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldServeBuiltinTemplateThumbnailFromClasspath() throws Exception {
        mockMvc.perform(get("/api/templates/thumbs/classic-single-thumb.svg").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/svg+xml"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<svg")));
    }

    @Test
    void shouldReturn404ForMissingThumbnail() throws Exception {
        mockMvc.perform(get("/api/templates/thumbs/nonexistent.svg").contextPath("/api"))
                .andExpect(status().isNotFound());
    }
}
