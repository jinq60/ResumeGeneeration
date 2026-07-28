package com.resume.resume.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 重命名简历响应。
 */
@Data
public class RenameResumeResponse {

    private String id;
    private String title;
    private LocalDateTime updatedAt;
}