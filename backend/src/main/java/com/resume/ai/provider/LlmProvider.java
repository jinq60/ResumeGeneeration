package com.resume.ai.provider;

import com.resume.ai.dto.AiChatRequest;
import com.resume.ai.dto.AiChatResponse;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import reactor.core.publisher.Flux;

/**
 * 统一文本 LLM 供应商接口。
 */
public interface LlmProvider {

    AiChatResponse chat(AiChatRequest request);

    /**
     * Stream text deltas when the provider supports server-sent generation.
     * The default keeps providers without streaming support compatible.
     */
    default Flux<String> stream(AiChatRequest request) {
        AiChatResponse response = chat(request);
        if (!response.isSuccess()) {
            return Flux.error(new BusinessException(ResultCode.AI_MODEL_CALL_FAILED,
                    response.getErrorMsg() == null ? "AI 模型调用失败。" : response.getErrorMsg()));
        }
        return Flux.just(response.getContent() == null ? "" : response.getContent());
    }

    <T> T chatStructured(AiChatRequest request, Class<T> responseType);

    boolean supportsModel(String modelName);

    String getProviderName();
}
