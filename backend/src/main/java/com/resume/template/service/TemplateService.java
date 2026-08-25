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
import com.resume.template.dto.TemplateStatsResponse;
import com.resume.template.entity.Template;
import com.resume.template.mapper.TemplateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
     * 获取模板实体（供渲染场景使用：分享页 / 预览 / PDF 导出）。
     * <p>
     * 容忍 inactive/deleted 模板：模板被删除或下架后，引用它的历史简历
     * （分享页、预览、PDF 导出）仍需正常渲染。编辑/选择模板场景请用
     * {@link #getTemplateEntity(String)} 保持严格校验。
     * </p>
     */
    public Template getTemplateEntityForRender(String templateId) {
        Template template = templateMapper.selectByIdIncludingDeleted(templateId);
        if (template == null) {
            throw new BusinessException(ResultCode.TEMPLATE_NOT_FOUND, "模板不存在。");
        }
        return template;
    }

    /**
     * 批量获取模板 ID -> 名称映射（供后台简历列表避免 N+1 查询）。
     */
    public Map<String, String> getTemplateNameMap(Collection<String> templateIds) {
        if (templateIds == null || templateIds.isEmpty()) {
            return Map.of();
        }
        LambdaQueryWrapper<Template> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Template::getId, templateIds)
                .eq(Template::getDeleted, BizConstant.NOT_DELETED);
        return templateMapper.selectList(wrapper).stream()
                .collect(Collectors.toMap(Template::getId, Template::getName, (a, b) -> a));
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
     * 后台模板统计。
     */
    public TemplateStatsResponse stats() {
        TemplateStatsResponse response = new TemplateStatsResponse();

        LambdaQueryWrapper<Template> notDeleted = new LambdaQueryWrapper<>();
        notDeleted.eq(Template::getDeleted, BizConstant.NOT_DELETED);
        response.setTotalTemplates(templateMapper.selectCount(notDeleted));

        LambdaQueryWrapper<Template> active = new LambdaQueryWrapper<>();
        active.eq(Template::getDeleted, BizConstant.NOT_DELETED)
                .eq(Template::getStatus, BizConstant.TEMPLATE_STATUS_ACTIVE);
        response.setActiveTemplates(templateMapper.selectCount(active));

        LambdaQueryWrapper<Template> inactive = new LambdaQueryWrapper<>();
        inactive.eq(Template::getDeleted, BizConstant.NOT_DELETED)
                .eq(Template::getStatus, BizConstant.TEMPLATE_STATUS_INACTIVE);
        response.setInactiveTemplates(templateMapper.selectCount(inactive));

        LambdaQueryWrapper<Template> builtin = new LambdaQueryWrapper<>();
        builtin.eq(Template::getDeleted, BizConstant.NOT_DELETED)
                .eq(Template::getIsBuiltin, BizConstant.BUILTIN_YES);
        response.setBuiltinTemplates(templateMapper.selectCount(builtin));

        return response;
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
     * <p>
     * uk_template_code 唯一索引不区分 deleted：若 code 命中已逻辑删除的行，
     * 直接插入新行必然触发 DuplicateKeyException（HTTP 500）。
     * 因此查重使用绕过逻辑删除的自定义 SQL，命中已删行时复活该行而不是插入新行。
     * </p>
     */
    @Transactional(rollbackFor = Exception.class)
    public TemplateDTO createTemplate(AdminTemplateRequest request, String operatorId) {
        // 查重需包含已删记录（自定义 SQL 绕过 @TableLogic）
        Template exist = findByCodeIncludingDeleted(request.getCode());
        validateConfig(request.getConfig());
        if (exist != null) {
            if (BizConstant.NOT_DELETED.equals(exist.getDeleted())) {
                throw new BusinessException(ResultCode.TEMPLATE_CODE_EXISTS, "模板编码已存在。");
            }
            // 复活被逻辑删除的模板行，回收其占用的唯一索引
            applyRequestFields(exist, request, operatorId);
            exist.setStatus(BizConstant.TEMPLATE_STATUS_ACTIVE);
            exist.setVersion(exist.getVersion() == null ? 1 : exist.getVersion() + 1);
            exist.setDeleted(BizConstant.NOT_DELETED);
            exist.setUpdatedAt(LocalDateTime.now());
            templateMapper.updateById(exist);
            return toAdminTemplateDTO(exist);
        }

        Template template = new Template();
        applyRequestFields(template, request, operatorId);
        template.setStatus(BizConstant.TEMPLATE_STATUS_ACTIVE);
        template.setVersion(1);
        template.setDeleted(BizConstant.NOT_DELETED);
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());
        templateMapper.insert(template);
        return toAdminTemplateDTO(template);
    }

    private void applyRequestFields(Template template, AdminTemplateRequest request, String operatorId) {
        template.setCode(request.getCode());
        template.setName(request.getName());
        template.setCategory(request.getCategory());
        template.setThumbnailUrl(request.getThumbnailUrl());
        template.setDescription(request.getDescription());
        template.setConfig(request.getConfig());
        template.setHtmlTemplate(request.getHtmlTemplate());
        template.setRenderEngine(StringUtils.defaultString(request.getRenderEngine(), BizConstant.RENDER_ENGINE_SERVER));
        template.setIsBuiltin(BizConstant.BUILTIN_NO);
        template.setIsPremium(BizConstant.BUILTIN_NO);
        template.setIsRecommended(request.getIsRecommended() != null && request.getIsRecommended()
                ? BizConstant.BUILTIN_YES : BizConstant.BUILTIN_NO);
        template.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        template.setCreatedBy(operatorId);
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
        validateConfig(request.getConfig());

        template.setName(request.getName());
        template.setCategory(request.getCategory());
        template.setThumbnailUrl(request.getThumbnailUrl());
        template.setDescription(request.getDescription());
        template.setConfig(request.getConfig());
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
     * <p>
     * 删除前检查简历引用：仍有未删除简历引用该模板时拒绝删除，
     * 否则这些简历的分享页/预览/PDF 导出会全部因模板缺失而报错。
     * </p>
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
        long references = templateMapper.countResumeReferences(templateId);
        if (references > 0) {
            // 注：ResultCode 属于 common 模块且无"模板被引用"专用错误码，
            // 此处复用 TEMPLATE_CODE_EXISTS(3001) 以获得统一的 409 资源冲突 HTTP 语义，
            // 具体原因通过 message 传达给前端。
            throw new BusinessException(ResultCode.TEMPLATE_CODE_EXISTS,
                    "该模板已被 " + references + " 份简历引用，无法删除。请先让相关简历更换模板。");
        }
        // deleteById 配合 @TableLogic 生成 UPDATE deleted=1；setDeleted+updateById 会因逻辑删除字段被
        // MP 排除在 SET 子句外而静默失效，禁止使用。
        template.setUpdatedAt(LocalDateTime.now());
        templateMapper.updateById(template);
        templateMapper.deleteById(templateId);
    }

    private void validateConfig(Object config) {
        if (config == null) {
            throw new BusinessException(ResultCode.TEMPLATE_CONFIG_INVALID, "模板配置为必填项。");
        }
        try {
            objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.TEMPLATE_CONFIG_INVALID, "模板配置格式不正确。", e);
        }
    }

    private Template findByCodeIncludingDeleted(String code) {
        // selectOne 会被 @TableLogic 追加 deleted=0，永远查不到已删行；
        // 必须用自定义 SQL 绕过逻辑删除
        return templateMapper.selectByCodeIncludingDeleted(code);
    }

    private boolean isValidTemplateStatus(String status) {
        return BizConstant.TEMPLATE_STATUS_ACTIVE.equals(status)
                || BizConstant.TEMPLATE_STATUS_INACTIVE.equals(status);
    }

    private TemplateDTO toTemplateDTO(Template template) {
        TemplateDTO dto = new TemplateDTO();
        dto.setId(template.getId());
        dto.setCode(template.getCode());
        dto.setName(template.getName());
        dto.setCategory(template.getCategory());
        dto.setThumbnailUrl(template.getThumbnailUrl());
        dto.setDescription(template.getDescription());
        dto.setConfig(template.getConfig());
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
        response.setThumbnailUrl(template.getThumbnailUrl());
        response.setDescription(template.getDescription());
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
