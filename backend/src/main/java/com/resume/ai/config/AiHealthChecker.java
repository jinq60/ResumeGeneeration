package com.resume.ai.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 应用启动时打印 AI 配置状态，便于运维排查。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiHealthChecker {

    private final AiProperties aiProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void checkAiConfig() {
        log.info("=== AI Provider Configuration ===");
        Map<String, AiProperties.FeatureConfig> providers = aiProperties.getProviders();
        if (providers == null || providers.isEmpty()) {
            log.warn("No AI providers configured — AI features will use placeholder data");
            return;
        }
        providers.forEach((feature, config) -> {
            boolean hasKey = hasApiKey(config.getProvider());
            log.info("  {} → {}/{} (timeout: {}, retry: {}) — {}",
                    feature, config.getProvider(), config.getModel(),
                    config.getTimeout(), config.getRetry(),
                    hasKey ? "available" : "NOT CONFIGURED (will fallback to placeholder)");
        });
    }

    private boolean hasApiKey(String provider) {
        return switch (provider) {
            case "openai" -> isNotEmpty(aiProperties.getOpenai().getApiKey());
            case "qwen" -> isNotEmpty(aiProperties.getQwen().getApiKey());
            case "ernie" -> isNotEmpty(aiProperties.getErnie().getApiKey());
            default -> false;
        };
    }

    private boolean isNotEmpty(String s) {
        return s != null && !s.isBlank();
    }
}
