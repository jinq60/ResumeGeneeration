package com.resume.ai.provider;

import com.resume.ai.dto.AiChatRequest;
import com.resume.ai.dto.AiChatResponse;

/**
 * 统一文本 LLM 供应商接口。
 */
public interface LlmProvider {

    AiChatResponse chat(AiChatRequest request);

    <T> T chatStructured(AiChatRequest request, Class<T> responseType);

    boolean supportsModel(String modelName);

    String getProviderName();
}
