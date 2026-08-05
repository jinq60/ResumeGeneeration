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

    @Test
    void shouldServeUploadsUnderContextPath() throws Exception {
        when(minioStorageService.getBucketAvatars()).thenReturn("resume-avatars");
        when(minioStorageService.download("resume-avatars", "user_1/avatars/a.png"))
                .thenReturn(new byte[]{1, 2, 3});

        // 未携带 token 也应放行（/uploads/** permitAll），且能到达控制器
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
