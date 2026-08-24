package com.resume.ai.dto;

import com.resume.ai.entity.AiRule;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * AI 规则响应。
 */
@Data
public class AiRuleResponse {

    private String id;
    private String familyId;
    private String name;
    private String ruleType;
    private String description;
    private String systemPrompt;
    private String userPrompt;
    private Map<String, Object> params;
    private String status;
    private Integer version;
    private LocalDateTime publishedAt;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AiRuleResponse from(AiRule rule) {
        AiRuleResponse response = new AiRuleResponse();
        response.setId(rule.getId());
        response.setFamilyId(rule.getFamilyId());
        response.setName(rule.getName());
        response.setRuleType(rule.getRuleType());
        response.setDescription(rule.getDescription());
        response.setSystemPrompt(rule.getSystemPrompt());
        response.setUserPrompt(rule.getUserPrompt());
        response.setParams(rule.getParams());
        response.setStatus(rule.getStatus());
        response.setVersion(rule.getVersion());
        response.setPublishedAt(rule.getPublishedAt());
        response.setCreatedBy(rule.getCreatedBy());
        response.setCreatedAt(rule.getCreatedAt());
        response.setUpdatedAt(rule.getUpdatedAt());
        return response;
    }
}