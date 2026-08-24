package com.resume.audit.dto;

import lombok.Data;

/**
 * 审核统计响应。
 */
@Data
public class AuditStatsResponse {

    private long total;
    private long pending;
    private long approved;
    private long rejected;
    private long warning;
    private long todayReviewed;

    public long byStatus(String status) {
        return switch (status) {
            case "pending" -> pending;
            case "approved" -> approved;
            case "rejected" -> rejected;
            case "warning" -> warning;
            default -> total;
        };
    }
}