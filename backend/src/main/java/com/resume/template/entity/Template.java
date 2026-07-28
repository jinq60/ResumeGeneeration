package com.resume.template.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 简历模板实体。
 */
@Data
@TableName("template")
public class Template {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String code;
    private String name;
    private String category;
    private String thumbnailUrl;
    private String description;

    private String config;

    private String htmlTemplate;
    private String renderEngine;
    private Integer isBuiltin;
    private Integer isPremium;
    private Integer isRecommended;
    private Integer sortOrder;
    private String status;
    private String createdBy;
    private Integer version;

    /**
     * 逻辑删除标记：0 未删除，1 已删除。
     */
    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
