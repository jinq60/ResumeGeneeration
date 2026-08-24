package com.resume.audit.dto;

import com.resume.audit.entity.ContentAudit;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审核记录响应。
 */
@Data
public class AuditItemResponse {

    private String id;
    private String targetType;
    private String targetId;
    private String targetTitle;
    private String userId;
    private String riskLevel;
    private String status;
    private String reviewerId;
    private String reviewNote;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AuditItemResponse from(ContentAudit audit) {
        AuditItemResponse response = new AuditItemResponse();
        response.setId(audit.getId());
        response.setTargetType(audit.getTargetType());
        response.setTargetId(audit.getTargetId());
        response.setTargetTitle(audit.getTargetTitle());
        response.setUserId(audit.getUserId());
        response.setRiskLevel(audit.getRiskLevel());
        response.setStatus(audit.getStatus());
        response.setReviewerId(audit.getReviewerId());
        response.setReviewNote(audit.getReviewNote());
        response.setReviewedAt(audit.getReviewedAt());
        response.setCreatedAt(audit.getCreatedAt());
        response.setUpdatedAt(audit.getUpdatedAt());
        return response;
    }
}