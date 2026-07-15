package com.resume.ai.provider;

import com.resume.ai.config.AiProperties;
import com.resume.ai.config.AiProperties.FeatureConfig;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 功能 → 厂商 → Provider 实现的路由器。
 */
@Component
@RequiredArgsConstructor
public class ProviderRouter {

    private final AiProperties aiProperties;
    private final List<LlmProvider> providers;

    private final Map<String, LlmProvider> providerCache = new ConcurrentHashMap<>();

    public LlmProvider resolve(String featureKey) {
        return providerCache.computeIfAbsent(featureKey, key -> {
            Map<String, FeatureConfig> providerConfigs = aiProperties.getProviders();
            if (providerConfigs == null || !providerConfigs.containsKey(key)) {
                throw new BusinessException(ResultCode.AI_PROVIDER_NOT_CONFIGURED,
                        "未配置 AI 功能对应的厂商: " + key);
            }

            FeatureConfig config = providerConfigs.get(key);
            String providerName = config.getProvider();

            return providers.stream()
                    .filter(p -> p.getProviderName().equalsIgnoreCase(providerName))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(ResultCode.AI_PROVIDER_NOT_CONFIGURED,
                            "未找到对应的 LLM 供应商实现: " + providerName));
        });
    }

    public String resolveModel(String featureKey) {
        Map<String, FeatureConfig> providerConfigs = aiProperties.getProviders();
        if (providerConfigs == null || !providerConfigs.containsKey(featureKey)) {
            throw new BusinessException(ResultCode.AI_PROVIDER_NOT_CONFIGURED,
                    "未配置 AI 功能对应的模型: " + featureKey);
        }
        return providerConfigs.get(featureKey).getModel();
    }

    public FeatureConfig getFeatureConfig(String featureKey) {
        Map<String, FeatureConfig> providerConfigs = aiProperties.getProviders();
        if (providerConfigs == null || !providerConfigs.containsKey(featureKey)) {
            throw new BusinessException(ResultCode.AI_PROVIDER_NOT_CONFIGURED,
                    "未配置 AI 功能: " + featureKey);
        }
        return providerConfigs.get(featureKey);
    }
}
