package com.resume.template.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 简历模板实体。
 */
@Data
@TableName(value = "template", autoResultMap = true)
public class Template {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String code;
    private String name;
    private String category;
    private String thumbnailUrl;
    private String description;

    /**
     * 模板配置 JSON。保持 Object + JacksonTypeHandler 以兼容 H2 测试库的 JSON 反序列化；
     * Service 层 via Map 视图操作，实际落库由 JacksonTypeHandler 负责。
     */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private Object config;

    private String htmlTemplate;
    private String renderEngine;
    private Integer isBuiltin;
    private Integer isPremium;
    private Integer isRecommended;
    private Integer sortOrder;
    private String status;
    private String createdBy;
    @Version
    private Integer version;

    /**
     * 逻辑删除标记：0 未删除，1 已删除。
     */
    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
