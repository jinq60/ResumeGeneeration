package com.resume.template.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.common.constant.BizConstant;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.template.dto.AdminTemplateListItemResponse;
import com.resume.template.dto.AdminTemplateRequest;
import com.resume.template.dto.TemplateDTO;
import com.resume.template.entity.Template;
import com.resume.template.mapper.TemplateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 模板业务服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateService {

    private final TemplateMapper templateMapper;
    private final ObjectMapper objectMapper;

    /**
     * 前台模板列表。
     */
    public List<TemplateDTO> listActiveTemplates() {
        LambdaQueryWrapper<Template> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Template::getStatus, BizConstant.TEMPLATE_STATUS_ACTIVE)
                .eq(Template::getDeleted, BizConstant.NOT_DELETED)
                .orderByAsc(Template::getSortOrder)
                .orderByDesc(Template::getCreatedAt);
        return templateMapper.selectList(wrapper).stream()
                .map(this::toTemplateDTO)
                .toList();
    }

    /**
     * 模板详情。
     */
    public TemplateDTO getTemplate(String templateId) {
        return toTemplateDTO(getTemplateEntity(templateId));
    }

    /**
     * 获取模板实体（供内部渲染使用）。
     */
    public Template getTemplateEntity(String templateId) {
        Template template = templateMapper.selectById(templateId);
        if (template == null || BizConstant.DELETED.equals(template.getDeleted())
                || !BizConstant.TEMPLATE_STATUS_ACTIVE.equals(template.getStatus())) {
            throw new BusinessException(ResultCode.TEMPLATE_NOT_FOUND, "模板不存在。");
        }
        return template;
    }

    /**
     * 后台模板列表。
     */
    public Page<AdminTemplateListItemResponse> listAdminTemplates(int page, int size, String status, String category) {
        LambdaQueryWrapper<Template> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Template::getDeleted, BizConstant.NOT_DELETED);
        if (StringUtils.isNotBlank(status)) {
            wrapper.eq(Template::getStatus, status);
        }
        if (StringUtils.isNotBlank(category)) {
            wrapper.eq(Template::getCategory, category);
        }
        wrapper.orderByAsc(Template::getSortOrder).orderByDesc(Template::getCreatedAt);

        Page<Template> pageParam = new Page<>(page, size);
        Page<Template> result = templateMapper.selectPage(pageParam, wrapper);

        List<AdminTemplateListItemResponse> list = result.getRecords().stream()
                .map(this::toAdminListItemResponse)
                .toList();
        Page<AdminTemplateListItemResponse> responsePage = new Page<>();
        responsePage.setRecords(list);
        responsePage.setTotal(result.getTotal());
        responsePage.setCurrent(result.getCurrent());
        responsePage.setSize(result.getSize());
        responsePage.setPages(result.getPages());
        return responsePage;
    }

    /**
     * 后台模板详情。
     */
    public TemplateDTO getAdminTemplate(String templateId) {
        Template template = templateMapper.selectById(templateId);
        if (template == null || BizConstant.DELETED.equals(template.getDeleted())) {
            throw new BusinessException(ResultCode.TEMPLATE_NOT_FOUND, "模板不存在。");
        }
        return toAdminTemplateDTO(template);
    }

    /**
     * 创建模板。
     */
    @Transactional(rollbackFor = Exception.class)
    public TemplateDTO createTemplate(AdminTemplateRequest request, String operatorId) {
        Template exist = findByCode(request.getCode());
        if (exist != null) {
            throw new BusinessException(ResultCode.TEMPLATE_CODE_EXISTS, "模板编码已存在。");
        }

        Template template = new Template();
        template.setCode(request.getCode());
        template.setName(request.getName());
        template.setCategory(request.getCategory());
        template.setThumbnailUrl(request.getThumbnailUrl());
        template.setDescription(request.getDescription());
        template.setConfig(toJson(request.getConfig()));
        template.setHtmlTemplate(request.getHtmlTemplate());
        template.setRenderEngine(StringUtils.defaultString(request.getRenderEngine(), BizConstant.RENDER_ENGINE_SERVER));
        template.setIsBuiltin(BizConstant.BUILTIN_NO);
        template.setIsPremium(BizConstant.BUILTIN_NO);
        template.setIsRecommended(request.getIsRecommended() != null && request.getIsRecommended()
                ? BizConstant.BUILTIN_YES : BizConstant.BUILTIN_NO);
        template.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        template.setStatus(BizConstant.TEMPLATE_STATUS_ACTIVE);
        template.setCreatedBy(operatorId);
        template.setVersion(1);
        template.setDeleted(BizConstant.NOT_DELETED);
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());
        templateMapper.insert(template);
        return toAdminTemplateDTO(template);
    }

    /**
     * 更新模板。
     */
    @Transactional(rollbackFor = Exception.class)
    public TemplateDTO updateTemplate(String templateId, AdminTemplateRequest request) {
        Template template = templateMapper.selectById(templateId);
        if (template == null || BizConstant.DELETED.equals(template.getDeleted())) {
            throw new BusinessException(ResultCode.TEMPLATE_NOT_FOUND, "模板不存在。");
        }
        if (!template.getCode().equals(request.getCode())) {
            throw new BusinessException(ResultCode.TEMPLATE_CODE_IMMUTABLE, "模板编码不可修改。");
        }

        template.setName(request.getName());
        template.setCategory(request.getCategory());
        template.setThumbnailUrl(request.getThumbnailUrl());
        template.setDescription(request.getDescription());
        template.setConfig(toJson(request.getConfig()));
        template.setHtmlTemplate(request.getHtmlTemplate());
        template.setRenderEngine(StringUtils.defaultString(request.getRenderEngine(), template.getRenderEngine()));
        template.setIsRecommended(request.getIsRecommended() != null && request.getIsRecommended()
                ? BizConstant.BUILTIN_YES : BizConstant.BUILTIN_NO);
        template.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : template.getSortOrder());
        template.setVersion(template.getVersion() + 1);
        template.setUpdatedAt(LocalDateTime.now());
        templateMapper.updateById(template);
        return toAdminTemplateDTO(template);
    }

    /**
     * 上下架模板。
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateTemplateStatus(String templateId, String status) {
        Template template = templateMapper.selectById(templateId);
        if (template == null || BizConstant.DELETED.equals(template.getDeleted())) {
            throw new BusinessException(ResultCode.TEMPLATE_NOT_FOUND, "模板不存在。");
        }
        if (!isValidTemplateStatus(status)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "模板状态仅支持 active 或 inactive。");
        }
        template.setStatus(status);
        template.setUpdatedAt(LocalDateTime.now());
        templateMapper.updateById(template);
    }

    /**
     * 删除模板。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(String templateId) {
        Template template = templateMapper.selectById(templateId);
        if (template == null || BizConstant.DELETED.equals(template.getDeleted())) {
            throw new BusinessException(ResultCode.TEMPLATE_NOT_FOUND, "模板不存在。");
        }
        if (BizConstant.BUILTIN_YES.equals(template.getIsBuiltin())) {
            throw new BusinessException(ResultCode.TEMPLATE_BUILTIN_PROTECTED, "系统内置模板不可删除。");
        }
        template.setDeleted(BizConstant.DELETED);
        template.setUpdatedAt(LocalDateTime.now());
        templateMapper.updateById(template);
    }

    private Template findByCode(String code) {
        LambdaQueryWrapper<Template> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Template::getCode, code)
                .eq(Template::getDeleted, BizConstant.NOT_DELETED);
        return templateMapper.selectOne(wrapper);
    }

    private boolean isValidTemplateStatus(String status) {
        return BizConstant.TEMPLATE_STATUS_ACTIVE.equals(status)
                || BizConstant.TEMPLATE_STATUS_INACTIVE.equals(status);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.TEMPLATE_CONFIG_INVALID, "模板配置格式不正确。");
        }
    }

    private Object toObject(String json) {
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "模板配置解析失败。");
        }
    }

    private TemplateDTO toTemplateDTO(Template template) {
        TemplateDTO dto = new TemplateDTO();
        dto.setId(template.getId());
        dto.setCode(template.getCode());
        dto.setName(template.getName());
        dto.setCategory(template.getCategory());
        dto.setThumbnailUrl(template.getThumbnailUrl());
        dto.setDescription(template.getDescription());
        dto.setConfig(toObject(template.getConfig()));
        dto.setRenderEngine(template.getRenderEngine());
        dto.setSortOrder(template.getSortOrder());
        dto.setIsRecommended(BizConstant.BUILTIN_YES.equals(template.getIsRecommended()));
        dto.setStatus(template.getStatus());
        dto.setCreatedAt(template.getCreatedAt());
        dto.setUpdatedAt(template.getUpdatedAt());
        return dto;
    }

    private TemplateDTO toAdminTemplateDTO(Template template) {
        TemplateDTO dto = toTemplateDTO(template);
        dto.setHtmlTemplate(template.getHtmlTemplate());
        return dto;
    }

    private AdminTemplateListItemResponse toAdminListItemResponse(Template template) {
        AdminTemplateListItemResponse response = new AdminTemplateListItemResponse();
        response.setId(template.getId());
        response.setCode(template.getCode());
        response.setName(template.getName());
        response.setCategory(template.getCategory());
        response.setStatus(template.getStatus());
        response.setIsBuiltin(BizConstant.BUILTIN_YES.equals(template.getIsBuiltin()));
        response.setIsRecommended(BizConstant.BUILTIN_YES.equals(template.getIsRecommended()));
        response.setSortOrder(template.getSortOrder());
        response.setCreatedBy(template.getCreatedBy());
        response.setCreatedAt(template.getCreatedAt());
        response.setUpdatedAt(template.getUpdatedAt());
        return response;
    }
}
