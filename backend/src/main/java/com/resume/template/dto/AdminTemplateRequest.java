package com.resume.template.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/**
 * 后台模板创建/更新请求。
 */
@Data
public class AdminTemplateRequest {

    @NotBlank(message = "模板编码为必填项。")
    @Size(max = 64, message = "模板编码过长。")
    @jakarta.validation.constraints.Pattern(regexp = "^[a-z0-9_-]+$", message = "模板编码仅允许小写字母、数字、下划线与连字符。")
    private String code;

    @NotBlank(message = "模板名称为必填项。")
    @Size(max = 64, message = "模板名称过长。")
    private String name;

    @NotBlank(message = "模板分类为必填项。")
    @Size(max = 32, message = "模板分类过长。")
    @jakarta.validation.constraints.Pattern(regexp = "^(classic|tech|fresh|business|postgraduate)$", message = "模板分类不正确。")
    private String category;

    @Size(max = 512, message = "缩略图地址过长。")
    @jakarta.validation.constraints.Pattern(regexp = "^(https?://|/uploads/).*", message = "缩略图地址格式不正确。")
    private String thumbnailUrl;

    @Size(max = 512, message = "模板描述过长。")
    private String description;

    /**
     * 模板配置：data-model-and-ddl.md §4.4 推荐 Map<String, Object> + JacksonTypeHandler。
     * 这里使用强类型 Map，前后端字段命名以 Spec 为准。
     */
    @NotNull(message = "模板配置为必填项。")
    private Map<String, Object> config;

    @NotBlank(message = "HTML 模板为必填项。")
    @Size(max = 128, message = "HTML 模板名称过长。")
    @jakarta.validation.constraints.Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "HTML 模板名不能包含路径分隔符。")
    private String htmlTemplate;

    @Size(max = 32, message = "渲染引擎过长。")
    @jakarta.validation.constraints.Pattern(regexp = "^(server|client|hybrid)$", message = "渲染引擎不正确。")
    private String renderEngine;

    @jakarta.validation.constraints.Min(value = 0, message = "排序权重不能为负。")
    @jakarta.validation.constraints.Max(value = 9999, message = "排序权重过大。")
    private Integer sortOrder;
    private Boolean isRecommended;
}
