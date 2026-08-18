package com.resume.ai.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Duration;
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

    /**
     * 单次调用超时；为空时由 Provider 使用默认值。
     */
    private Duration timeout;

    /**
     * 重试次数；为空时由 Provider 使用默认值。
     */
    private Integer retry;

    @Data
    @Builder
    public static class Message {
        private String role;
        private String content;

        /**
         * 多模态内容（图片 + 文本）；非空时优先于 {@link #content}，
         * 按 OpenAI/Qwen 视觉模型的 content 数组格式序列化。
         */
        private List<ContentPart> contents;
    }

    /**
     * 多模态消息片段：文本或图片（data URL / 可访问 URL）。
     */
    @Data
    @Builder
    public static class ContentPart {
        private String type;
        private String text;
        private String imageUrl;
    }
}
