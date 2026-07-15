package com.resume.ai.provider.qwen;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.ai.config.AiProperties;
import com.resume.ai.dto.AiChatRequest;
import com.resume.ai.dto.AiChatResponse;
import com.resume.ai.provider.LlmProvider;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component("qwenLlmProvider")
@RequiredArgsConstructor
public class QwenLlmProvider implements LlmProvider {

    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    private WebClient webClient;

    private WebClient getClient() {
        if (webClient == null) {
            webClient = WebClient.builder()
                    .baseUrl(aiProperties.getQwen().getBaseUrl())
                    .defaultHeader("Authorization", "Bearer " + aiProperties.getQwen().getApiKey())
                    .build();
        }
        return webClient;
    }

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        long start = System.currentTimeMillis();
        try {
            Map<String, Object> body = buildRequestBody(request);
            String raw = getClient().post()
                    .uri("/compatible-mode/v1/chat/completions")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .retryWhen(Retry.backoff(2, Duration.ofSeconds(1)))
                    .block(Duration.ofSeconds(120));

            return parseResponse(raw, start);
        } catch (WebClientRequestException e) {
            throw new BusinessException(ResultCode.AI_MODEL_CALL_FAILED,
                    "Qwen 调用失败: " + e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T chatStructured(AiChatRequest request, Class<T> responseType) {
        AiChatResponse response = chat(request);
        if (!response.isSuccess()) {
            throw new BusinessException(ResultCode.AI_RESPONSE_PARSE_FAILED,
                    "AI 调用失败: " + response.getErrorMsg());
        }
        try {
            String content = response.getContent().trim();
            if (content.startsWith("```")) {
                content = content.replaceAll("```\\w*\\n?", "").replaceAll("```\\n?", "").trim();
            }
            return objectMapper.readValue(content, responseType);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse structured response: {}", response.getContent(), e);
            throw new BusinessException(ResultCode.AI_RESPONSE_PARSE_FAILED,
                    "AI 响应格式解析失败: " + e.getMessage());
        }
    }

    @Override
    public boolean supportsModel(String modelName) {
        return modelName != null && (modelName.startsWith("qwen-") || modelName.equals("qwq")
                || modelName.startsWith("qvq"));
    }

    @Override
    public String getProviderName() {
        return "qwen";
    }

    private Map<String, Object> buildRequestBody(AiChatRequest request) {
        List<Map<String, String>> messages;
        if (request.getMessages() != null && !request.getMessages().isEmpty()) {
            messages = request.getMessages().stream()
                    .map(m -> Map.of("role", m.getRole(), "content", m.getContent()))
                    .toList();
        } else {
            messages = List.of(
                    Map.of("role", "system", "content", request.getSystemPrompt()),
                    Map.of("role", "user", "content", request.getUserPrompt())
            );
        }

        Map<String, Object> body = new java.util.HashMap<>();
        body.put("model", request.getModel());
        body.put("messages", messages);
        body.put("temperature", request.getTemperature() != null ? request.getTemperature() : 0.7);
        if (request.getMaxTokens() != null) {
            body.put("max_tokens", request.getMaxTokens());
        }
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("result_format", "message");
        body.put("parameters", params);
        return body;
    }

    private AiChatResponse parseResponse(String raw, long start) {
        AiChatResponse response = new AiChatResponse();
        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                response.setContent(choices.get(0).get("message").get("content").asText());
            }
            response.setModel(root.get("model").asText());

            JsonNode usage = root.get("usage");
            if (usage != null) {
                response.setPromptTokens(usage.get("input_tokens").asInt());
                response.setCompletionTokens(usage.get("output_tokens").asInt());
                JsonNode total = usage.get("total_tokens");
                response.setTotalTokens(total != null ? total.asInt()
                        : response.getPromptTokens() + response.getCompletionTokens());
            }
            response.setSuccess(true);
        } catch (Exception e) {
            log.error("Failed to parse Qwen response: {}", raw, e);
            response.setSuccess(false);
            response.setErrorMsg("响应解析失败: " + e.getMessage());
        }
        response.setLatencyMs(System.currentTimeMillis() - start);
        return response;
    }
}
