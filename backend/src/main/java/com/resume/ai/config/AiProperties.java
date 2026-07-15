package com.resume.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * AI/LLM 多厂商配置绑定。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    private Map<String, FeatureConfig> providers;

    private OpenAiConfig openai;
    private QwenConfig qwen;
    private ErnieConfig ernie;
    private ThreadPoolConfig threadPool = new ThreadPoolConfig();
    private RateLimitConfig rateLimit = new RateLimitConfig();
    private Map<String, String> prompts;

    @Data
    public static class FeatureConfig {
        private String provider;
        private String model;
        private String timeout = "60s";
        private int retry = 2;
    }

    @Data
    public static class OpenAiConfig {
        private String apiKey;
        private String baseUrl = "https://api.openai.com";
    }

    @Data
    public static class QwenConfig {
        private String apiKey;
        private String baseUrl = "https://dashscope.aliyuncs.com";
    }

    @Data
    public static class ErnieConfig {
        private String apiKey;
        private String secretKey;
        private String baseUrl = "https://aip.baidubce.com";
    }

    @Data
    public static class ThreadPoolConfig {
        private int coreSize = 4;
        private int maxSize = 8;
        private int queueCapacity = 100;
    }

    @Data
    public static class RateLimitConfig {
        private int maxConcurrentPerUser = 3;
    }
}
