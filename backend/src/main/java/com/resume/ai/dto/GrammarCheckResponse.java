package com.resume.ai.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 简历语法检查结果。
 */
@Data
public class GrammarCheckResponse {

    private String status;
    private String model;
    private String message;
    private List<GrammarIssue> issues = List.of();
    private LocalDateTime checkedAt;

    @Data
    public static class GrammarIssue {
        private String sectionType;
        private String field;
        private Integer itemIndex;
        private String severity;
        private String originalText;
        private String suggestion;
        private String explanation;
    }
}
