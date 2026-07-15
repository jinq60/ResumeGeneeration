package com.resume.resume.entity;

import lombok.Data;

/**
 * AI 简历点评建议项。
 */
@Data
public class ResumeReviewSuggestion {

    private String sectionType;
    private String title;
    private String problem;
    private String advice;
    private String priority;
}
