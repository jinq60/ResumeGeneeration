package com.resume.template.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.common.config.TestSecurityConfig;
import com.resume.common.service.MinioStorageService;
import com.resume.template.dto.AdminTemplateRequest;
import com.resume.template.dto.TemplateDTO;
import com.resume.template.service.TemplateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminTemplateController.class)
@Import(TestSecurityConfig.class)
class AdminTemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TemplateService templateService;

    @MockBean
    private MinioStorageService minioStorageService;

    @Test
    void create_shouldReturnNewTemplate_whenAdmin() throws Exception {
        AdminTemplateRequest request = buildRequest("tpl_new");
        TemplateDTO dto = buildDto("tpl_1", "tpl_new");

        when(templateService.createTemplate(any(), any())).thenReturn(dto);

        mockMvc.perform(post("/admin/templates")
                .with(csrf())
                .with(user("admin_1").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("tpl_1"));
    }

    @Test
    void create_shouldRejectNonAdmin() throws Exception {
        AdminTemplateRequest request = buildRequest("tpl_new");

        mockMvc.perform(post("/admin/templates")
                .with(csrf())
                .with(user("normal").roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_shouldRejectMissingCode() throws Exception {
        AdminTemplateRequest request = new AdminTemplateRequest();
        request.setName("Name");
        request.setCategory("classic");
        request.setConfig(Map.of("page", Map.of("width", "210mm")));
        request.setHtmlTemplate("classic.html");

        mockMvc.perform(post("/admin/templates")
                .with(csrf())
                .with(user("admin_1").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateStatus_shouldSucceed() throws Exception {
        mockMvc.perform(patch("/admin/templates/tpl_1/status")
                .with(csrf())
                .with(user("admin_1").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"inactive\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void updateStatus_shouldRejectNonAdmin() throws Exception {
        mockMvc.perform(patch("/admin/templates/tpl_1/status")
                .with(csrf())
                .with(user("normal").roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"inactive\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_shouldSucceed() throws Exception {
        mockMvc.perform(delete("/admin/templates/tpl_1")
                .with(csrf())
                .with(user("admin_1").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    private AdminTemplateRequest buildRequest(String code) {
        AdminTemplateRequest request = new AdminTemplateRequest();
        request.setCode(code);
        request.setName("Template Name");
        request.setCategory("classic");
        request.setDescription("desc");
        request.setConfig(Map.of("page", Map.of("width", "210mm")));
        request.setHtmlTemplate("classic.html");
        request.setRenderEngine("server");
        request.setSortOrder(10);
        request.setIsRecommended(false);
        return request;
    }

    private TemplateDTO buildDto(String id, String code) {
        TemplateDTO dto = new TemplateDTO();
        dto.setId(id);
        dto.setCode(code);
        dto.setName("Template Name");
        dto.setCategory("classic");
        dto.setRenderEngine("server");
        dto.setStatus("active");
        dto.setSortOrder(10);
        dto.setIsRecommended(false);
        dto.setCreatedAt(LocalDateTime.now());
        dto.setUpdatedAt(LocalDateTime.now());
        return dto;
    }
}