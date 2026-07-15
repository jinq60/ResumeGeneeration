package com.resume.ai.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * JD 匹配优化响应。
 */
@Data
public class ResumeOptimizeResponse {

    private String taskId;
    private String resumeId;
    private Integer matchScore;
    private Map<String, Integer> dimensionScores;
    private List<SectionOptimization> optimizations;
    private List<String> missingSkills;
    private List<String> recommendations;
    private String status;
    private String errorMsg;
    private LocalDateTime createdAt;

    @Data
    public static class SectionOptimization {
        private String sectionId;
        private String sectionType;
        private String originalSummary;
        private String optimizedContent;
        private String reasoning;
    }
}
