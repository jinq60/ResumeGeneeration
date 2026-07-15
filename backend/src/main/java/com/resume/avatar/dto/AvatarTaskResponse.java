package com.resume.avatar.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 头像优化任务响应。
 */
@Data
public class AvatarTaskResponse {

    private String taskId;
    private String status;
    private String sourceImageUrl;
    private String resultImageUrl;
    private String errorMsg;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
