package com.resume.resume.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI 简历点评响应。
 */
@Data
public class ResumeReviewResponse {

    private String reviewId;
    private String resumeId;
    private Integer overallScore;
    private Map<String, Integer> dimensionScores;
    private List<SuggestionDTO> suggestions;
    private List<String> highlights;
    private LocalDateTime createdAt;

    @Data
    public static class SuggestionDTO {
        private String sectionType;
        private String title;
        private String problem;
        private String advice;
        private String priority;
    }
}
