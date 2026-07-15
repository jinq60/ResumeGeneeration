package com.resume.pdf.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * PDF 任务响应。
 */
@Data
public class PdfTaskResponse {

    private String taskId;
    private String resumeId;
    private String templateId;
    private String status;
    private String fileName;
    private Long fileSize;
    private String errorMsg;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
