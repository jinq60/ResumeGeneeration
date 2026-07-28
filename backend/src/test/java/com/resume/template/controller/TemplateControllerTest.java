package com.resume.template.controller;

import com.resume.common.config.TestSecurityConfig;
import com.resume.template.dto.TemplateDTO;
import com.resume.template.service.TemplateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TemplateController.class)
@Import(TestSecurityConfig.class)
class TemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TemplateService templateService;

    @Test
    void list_shouldReturnActiveTemplates() throws Exception {
        TemplateDTO tpl = buildTemplate("tpl_1", "classic-single");
        when(templateService.listActiveTemplates()).thenReturn(List.of(tpl));

        mockMvc.perform(get("/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value("tpl_1"))
                .andExpect(jsonPath("$.data[0].code").value("classic-single"));
    }

    @Test
    void get_shouldReturnTemplateDetail() throws Exception {
        TemplateDTO tpl = buildTemplate("tpl_1", "classic-single");
        when(templateService.getTemplate("tpl_1")).thenReturn(tpl);

        mockMvc.perform(get("/templates/tpl_1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("tpl_1"));
    }

    @Test
    void get_shouldReturnNotFoundWhenMissing() throws Exception {
        when(templateService.getTemplate("missing"))
                .thenThrow(new com.resume.common.exception.BusinessException(
                        com.resume.common.constant.ResultCode.TEMPLATE_NOT_FOUND, "模板不存在。"));

        mockMvc.perform(get("/templates/missing"))
                .andExpect(status().isNotFound());
    }

    private TemplateDTO buildTemplate(String id, String code) {
        TemplateDTO dto = new TemplateDTO();
        dto.setId(id);
        dto.setCode(code);
        dto.setName("经典单栏");
        dto.setCategory("classic");
        dto.setRenderEngine("server");
        dto.setSortOrder(10);
        dto.setIsRecommended(true);
        dto.setStatus("active");
        dto.setCreatedAt(LocalDateTime.now());
        dto.setUpdatedAt(LocalDateTime.now());
        return dto;
    }
}