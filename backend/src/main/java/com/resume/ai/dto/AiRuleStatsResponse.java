package com.resume.ai.dto;

import lombok.Data;

/**
 * AI 规则统计响应。
 */
@Data
public class AiRuleStatsResponse {

    private long total;
    private long active;
    private long disabled;
    private long draft;
    private long todayPublished;
}