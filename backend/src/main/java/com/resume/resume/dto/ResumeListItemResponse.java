package com.resume.resume.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 简历列表项响应。
 */
@Data
public class ResumeListItemResponse {

    private String id;
    private String title;
    private String scene;
    private String targetPosition;
    private String templateId;
    private LocalDateTime lastEditedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
