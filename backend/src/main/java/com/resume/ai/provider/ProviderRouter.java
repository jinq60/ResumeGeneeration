package com.resume.ai.provider;

import com.resume.ai.config.AiProperties;
import com.resume.ai.config.AiProperties.FeatureConfig;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 功能 → 厂商 → Provider 实现的路由器。
 * <p>
 * 不做永久缓存：配置为外置（环境变量/配置中心）时需即时生效，避免热更新后仍命中旧供应商。
 * 解析开销极低（遍历 2~3 个 Provider），无需缓存。
 * </p>
 */
@Component
@RequiredArgsConstructor
public class ProviderRouter {

    private final AiProperties aiProperties;
    private final List<LlmProvider> providers;

    public LlmProvider resolve(String featureKey) {
        Map<String, FeatureConfig> providerConfigs = aiProperties.getProviders();
        if (providerConfigs == null || !providerConfigs.containsKey(featureKey)) {
            throw new BusinessException(ResultCode.AI_PROVIDER_NOT_CONFIGURED,
                    "未配置 AI 功能对应的厂商: " + featureKey);
        }
        FeatureConfig config = providerConfigs.get(featureKey);
        String providerName = config.getProvider();
        return providers.stream()
                .filter(p -> p.getProviderName().equalsIgnoreCase(providerName))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.AI_PROVIDER_NOT_CONFIGURED,
                        "未找到对应的 LLM 供应商实现: " + providerName));
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
