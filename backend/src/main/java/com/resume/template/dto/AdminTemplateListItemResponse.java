package com.resume.template.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台模板列表项响应。
 */
@Data
public class AdminTemplateListItemResponse {

    private String id;
    private String code;
    private String name;
    private String category;
    private String thumbnailUrl;
    private String description;
    private String status;
    private Boolean isBuiltin;
    private Boolean isRecommended;
    private Integer sortOrder;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
