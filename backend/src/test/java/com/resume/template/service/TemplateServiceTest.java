package com.resume.template.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.common.constant.BizConstant;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.template.dto.AdminTemplateRequest;
import com.resume.template.dto.TemplateDTO;
import com.resume.template.entity.Template;
import com.resume.template.mapper.TemplateMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TemplateServiceTest {

    @Mock
    private TemplateMapper templateMapper;

    private TemplateService templateService;

    @BeforeEach
    void setUp() {
        templateService = new TemplateService(templateMapper, new ObjectMapper());
    }

    @Test
    void createTemplate_shouldSucceed() {
        AdminTemplateRequest request = buildRequest("classic-new");
        when(templateMapper.selectOne(any())).thenReturn(null);
        when(templateMapper.insert(any(Template.class))).thenAnswer(inv -> {
            Template t = inv.getArgument(0);
            t.setId("tpl_new");
            return 1;
        });

        TemplateDTO dto = templateService.createTemplate(request, "admin_1");

        assertEquals("tpl_new", dto.getId());
        assertEquals("classic-new", dto.getCode());
        assertEquals(BizConstant.TEMPLATE_STATUS_ACTIVE, dto.getStatus());
        verify(templateMapper).insert(any(Template.class));
    }

    @Test
    void createTemplate_shouldRejectDuplicateCode() {
        AdminTemplateRequest request = buildRequest("classic-existing");
        Template existing = new Template();
        existing.setCode("classic-existing");
        existing.setDeleted(BizConstant.NOT_DELETED);
        when(templateMapper.selectOne(any())).thenReturn(existing);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> templateService.createTemplate(request, "admin_1"));
        assertEquals(ResultCode.TEMPLATE_CODE_EXISTS, ex.getErrorCode());
    }

    @Test
    void createTemplate_shouldRejectInvalidConfig() {
        AdminTemplateRequest request = buildRequest("classic-invalid-config");
        request.setConfig(new NonSerializableConfig());
        when(templateMapper.selectOne(any())).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> templateService.createTemplate(request, "admin_1"));
        assertEquals(ResultCode.TEMPLATE_CONFIG_INVALID, ex.getErrorCode());
    }

    @Test
    void updateTemplate_shouldSucceed() {
        Template template = buildTemplate("tpl_1", "classic-1");
        when(templateMapper.selectById("tpl_1")).thenReturn(template);

        AdminTemplateRequest request = buildRequest("classic-1");
        request.setName("Updated Name");

        TemplateDTO dto = templateService.updateTemplate("tpl_1", request);

        assertEquals("Updated Name", dto.getName());
        assertEquals(2, template.getVersion());
        verify(templateMapper).updateById(template);
    }

    @Test
    void updateTemplate_shouldRejectCodeChange() {
        Template template = buildTemplate("tpl_1", "classic-1");
        when(templateMapper.selectById("tpl_1")).thenReturn(template);

        AdminTemplateRequest request = buildRequest("classic-2");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> templateService.updateTemplate("tpl_1", request));
        assertEquals(ResultCode.TEMPLATE_CODE_IMMUTABLE, ex.getErrorCode());
    }

    @Test
    void updateTemplateStatus_shouldAcceptActive() {
        Template template = buildTemplate("tpl_1", "classic-1");
        template.setStatus(BizConstant.TEMPLATE_STATUS_INACTIVE);
        when(templateMapper.selectById("tpl_1")).thenReturn(template);

        templateService.updateTemplateStatus("tpl_1", BizConstant.TEMPLATE_STATUS_ACTIVE);

        assertEquals(BizConstant.TEMPLATE_STATUS_ACTIVE, template.getStatus());
        verify(templateMapper).updateById(template);
    }

    @Test
    void updateTemplateStatus_shouldAcceptInactive() {
        Template template = buildTemplate("tpl_1", "classic-1");
        template.setStatus(BizConstant.TEMPLATE_STATUS_ACTIVE);
        when(templateMapper.selectById("tpl_1")).thenReturn(template);

        templateService.updateTemplateStatus("tpl_1", BizConstant.TEMPLATE_STATUS_INACTIVE);

        assertEquals(BizConstant.TEMPLATE_STATUS_INACTIVE, template.getStatus());
    }

    @Test
    void updateTemplateStatus_shouldRejectInvalidStatus() {
        Template template = buildTemplate("tpl_1", "classic-1");
        when(templateMapper.selectById("tpl_1")).thenReturn(template);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> templateService.updateTemplateStatus("tpl_1", "deleted"));
        assertEquals(ResultCode.PARAM_INVALID, ex.getErrorCode());
    }

    @Test
    void deleteTemplate_shouldRejectBuiltin() {
        Template template = buildTemplate("tpl_1", "builtin-1");
        template.setIsBuiltin(BizConstant.BUILTIN_YES);
        when(templateMapper.selectById("tpl_1")).thenReturn(template);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> templateService.deleteTemplate("tpl_1"));
        assertEquals(ResultCode.TEMPLATE_BUILTIN_PROTECTED, ex.getErrorCode());
    }

    @Test
    void deleteTemplate_shouldSucceed() {
        Template template = buildTemplate("tpl_1", "custom-1");
        template.setIsBuiltin(BizConstant.BUILTIN_NO);
        when(templateMapper.selectById("tpl_1")).thenReturn(template);

        templateService.deleteTemplate("tpl_1");

        assertEquals(BizConstant.DELETED, template.getDeleted());
        verify(templateMapper).updateById(template);
    }

    @Test
    void listAdminTemplates_shouldFilterByStatus() {
        Page<Template> pageResult = new Page<>();
        pageResult.setRecords(List.of(buildTemplate("tpl_1", "c1")));
        pageResult.setTotal(1);
        pageResult.setCurrent(1);
        pageResult.setSize(20);
        pageResult.setPages(1);

        when(templateMapper.selectPage(any(Page.class), any())).thenReturn(pageResult);

        var response = templateService.listAdminTemplates(1, 20, BizConstant.TEMPLATE_STATUS_ACTIVE, null);

        assertEquals(1, response.getTotal());
        assertEquals(1, response.getRecords().size());
    }

    private AdminTemplateRequest buildRequest(String code) {
        AdminTemplateRequest request = new AdminTemplateRequest();
        request.setCode(code);
        request.setName("Template Name");
        request.setCategory("classic");
        request.setThumbnailUrl("/thumb.png");
        request.setDescription("desc");
        request.setConfig(Map.of("page", Map.of("width", "210mm")));
        request.setHtmlTemplate("classic.html");
        request.setRenderEngine(BizConstant.RENDER_ENGINE_SERVER);
        request.setSortOrder(10);
        request.setIsRecommended(false);
        return request;
    }

    private Template buildTemplate(String id, String code) {
        Template template = new Template();
        template.setId(id);
        template.setCode(code);
        template.setName("Template");
        template.setCategory("classic");
        template.setConfig(Map.of());
        template.setHtmlTemplate("classic.html");
        template.setRenderEngine(BizConstant.RENDER_ENGINE_SERVER);
        template.setIsBuiltin(BizConstant.BUILTIN_NO);
        template.setStatus(BizConstant.TEMPLATE_STATUS_ACTIVE);
        template.setVersion(1);
        template.setDeleted(BizConstant.NOT_DELETED);
        return template;
    }

    /**
     * 用于测试不可 JSON 序列化的配置对象。
     */
    private static class NonSerializableConfig {
        @SuppressWarnings("unused")
        private final NonSerializableConfig self = this;
    }
}
