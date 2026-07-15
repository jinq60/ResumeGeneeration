package com.resume.template.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模板列表/详情响应。
 */
@Data
public class TemplateDTO {

    private String id;
    private String code;
    private String name;
    private String category;
    private String thumbnailUrl;
    private String description;
    private Object config;
    private String htmlTemplate;
    private String renderEngine;
    private Integer sortOrder;
    private Boolean isRecommended;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
