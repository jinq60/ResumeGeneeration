package com.resume.resume.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台简历列表项响应。
 */
@Data
public class AdminResumeListItemResponse {

    private String id;
    private String userId;
    private String title;
    private String scene;
    private String targetPosition;
    private String templateId;
    private String templateName;
    private Integer exportCount;
    private String status;
    private LocalDateTime lastEditedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
