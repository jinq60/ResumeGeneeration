package com.resume.ai.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * LLM 统一对话请求。
 */
@Data
@Builder
public class AiChatRequest {

    private String model;

    private String systemPrompt;

    private String userPrompt;

    private List<Message> messages;

    private Double temperature;

    private Integer maxTokens;

    @Data
    @Builder
    public static class Message {
        private String role;
        private String content;
    }
}
