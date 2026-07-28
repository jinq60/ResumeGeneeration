package com.resume.resume.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 更新简历响应。
 */
@Data
public class UpdateResumeResponse {

    private String id;
    private LocalDateTime updatedAt;
}