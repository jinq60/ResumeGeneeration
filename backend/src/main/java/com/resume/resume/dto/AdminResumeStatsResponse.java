package com.resume.resume.dto;

import lombok.Data;

/**
 * 后台简历统计响应。
 */
@Data
public class AdminResumeStatsResponse {

    private Long totalResumes;
    private Long activeResumes;
    private Long deletedResumes;
    private Long todayNewResumes;
}
