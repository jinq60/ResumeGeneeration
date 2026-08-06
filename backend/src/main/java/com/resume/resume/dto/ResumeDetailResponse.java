package com.resume.resume.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 简历详情响应。
 */
@Data
public class ResumeDetailResponse {

    private String id;
    private String userId;
    private String title;
    private String scene;
    private String targetPosition;
    private String targetIndustry;
    private String templateId;
    private List<SectionDTO> sections;
    private RenderSettings renderSettings;
    private Integer exportCount;
    private LocalDateTime lastEditedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
