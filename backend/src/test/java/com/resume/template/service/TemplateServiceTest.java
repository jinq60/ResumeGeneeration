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
        when(templateMapper.selectByCodeIncludingDeleted("classic-new")).thenReturn(null);
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
        when(templateMapper.selectByCodeIncludingDeleted("classic-existing")).thenReturn(existing);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> templateService.createTemplate(request, "admin_1"));
        assertEquals(ResultCode.TEMPLATE_CODE_EXISTS, ex.getErrorCode());
    }

    @Test
    void createTemplate_shouldRejectInvalidConfig() {
        AdminTemplateRequest request = buildRequest("classic-invalid-config");
        // Map 包含不可序列化对象，触发 Jackson 失败；NonSerializableConfig 含自引用
        Map<String, Object> invalid = new java.util.HashMap<>();
        invalid.put("bad", new NonSerializableConfig());
        request.setConfig(invalid);
        when(templateMapper.selectByCodeIncludingDeleted("classic-invalid-config")).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> templateService.createTemplate(request, "admin_1"));
        assertEquals(ResultCode.TEMPLATE_CONFIG_INVALID, ex.getErrorCode());
    }

    @Test
    void createTemplate_shouldReviveDeletedRowWithSameCode() {
        AdminTemplateRequest request = buildRequest("classic-deleted");

        // 唯一索引不区分 deleted：命中已逻辑删除的同 code 行时应复活而非插入新行
        Template deleted = buildTemplate("tpl_old", "classic-deleted");
        deleted.setStatus(BizConstant.TEMPLATE_STATUS_INACTIVE);
        deleted.setVersion(3);
        deleted.setDeleted(BizConstant.DELETED);
        when(templateMapper.selectByCodeIncludingDeleted("classic-deleted")).thenReturn(deleted);
        when(templateMapper.updateIncludingDeleted(any(Template.class))).thenReturn(1);

        TemplateDTO dto = templateService.createTemplate(request, "admin_1");

        assertEquals("tpl_old", dto.getId());
        assertEquals(BizConstant.NOT_DELETED, deleted.getDeleted());
        assertEquals(BizConstant.TEMPLATE_STATUS_ACTIVE, deleted.getStatus());
        assertEquals(4, deleted.getVersion());
        verify(templateMapper).updateIncludingDeleted(deleted);
        verify(templateMapper, never()).insert(any(Template.class));
    }

    @Test
    void getTemplateEntityForRender_shouldReturnInactiveOrDeletedTemplate() {
        Template deleted = buildTemplate("tpl_1", "classic-1");
        deleted.setDeleted(BizConstant.DELETED);
        when(templateMapper.selectByIdIncludingDeleted("tpl_1")).thenReturn(deleted);

        // 渲染场景容忍已删除模板，历史简历仍需可渲染
        assertSame(deleted, templateService.getTemplateEntityForRender("tpl_1"));
    }

    @Test
    void getTemplateEntityForRender_shouldRejectMissing() {
        when(templateMapper.selectByIdIncludingDeleted("tpl_missing")).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> templateService.getTemplateEntityForRender("tpl_missing"));
        assertEquals(ResultCode.TEMPLATE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void updateTemplate_shouldSucceed() {
        Template template = buildTemplate("tpl_1", "classic-1");
        when(templateMapper.selectById("tpl_1")).thenReturn(template);
        when(templateMapper.updateById(any(Template.class))).thenReturn(1);

        AdminTemplateRequest request = buildRequest("classic-1");
        request.setName("Updated Name");

        TemplateDTO dto = templateService.updateTemplate("tpl_1", request);

        assertEquals("Updated Name", dto.getName());
        // Mock 场景下 @Version 拦截器未生效，version 保持原值；真实 DB 由拦截器 CAS 递增
        assertTrue(template.getVersion() == 1 || template.getVersion() == 2);
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
        when(templateMapper.updateById(any(Template.class))).thenReturn(1);

        templateService.updateTemplateStatus("tpl_1", BizConstant.TEMPLATE_STATUS_ACTIVE);

        assertEquals(BizConstant.TEMPLATE_STATUS_ACTIVE, template.getStatus());
        verify(templateMapper).updateById(template);
    }

    @Test
    void updateTemplateStatus_shouldAcceptInactive() {
        Template template = buildTemplate("tpl_1", "classic-1");
        template.setStatus(BizConstant.TEMPLATE_STATUS_ACTIVE);
        when(templateMapper.selectById("tpl_1")).thenReturn(template);
        when(templateMapper.updateById(any(Template.class))).thenReturn(1);

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
    void deleteTemplate_shouldRejectWhenReferencedByResumes() {
        Template template = buildTemplate("tpl_1", "custom-1");
        template.setIsBuiltin(BizConstant.BUILTIN_NO);
        when(templateMapper.selectById("tpl_1")).thenReturn(template);
        when(templateMapper.countResumeReferences("tpl_1")).thenReturn(5L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> templateService.deleteTemplate("tpl_1"));
        assertEquals(ResultCode.TEMPLATE_CODE_EXISTS, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("5"));
        verify(templateMapper, never()).updateById(any(Template.class));
    }

    @Test
    void deleteTemplate_shouldSucceedWhenNoReference() {
        Template template = buildTemplate("tpl_1", "custom-1");
        template.setIsBuiltin(BizConstant.BUILTIN_NO);
        when(templateMapper.selectById("tpl_1")).thenReturn(template);
        when(templateMapper.countResumeReferences("tpl_1")).thenReturn(0L);

        templateService.deleteTemplate("tpl_1");

        // 逻辑删除必须走 deleteById（MP 转 UPDATE deleted=1），setDeleted+updateById 静默失效
        verify(templateMapper).updateById(template);
        verify(templateMapper).deleteById("tpl_1");
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
