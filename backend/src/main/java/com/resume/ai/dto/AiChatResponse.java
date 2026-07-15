package com.resume.ai.dto;

import lombok.Data;

/**
 * LLM 统一对话响应。
 */
@Data
public class AiChatResponse {

    private String content;

    private String model;

    private Integer promptTokens;

    private Integer completionTokens;

    private Integer totalTokens;

    private long latencyMs;

    private boolean success;

    private String errorMsg;
}
