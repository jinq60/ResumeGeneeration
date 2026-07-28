package com.resume.resume.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 复制简历响应。
 */
@Data
public class DuplicateResumeResponse {

    private String id;
    private String title;
    private LocalDateTime createdAt;
}