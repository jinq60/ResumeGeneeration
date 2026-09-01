package com.resume.ai.provider.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
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
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component("openAiLlmProvider")
@RequiredArgsConstructor
public class OpenAiLlmProvider implements LlmProvider {

    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    private volatile WebClient webClient;

    private WebClient getClient() {
        WebClient client = webClient;
        if (client != null) {
            return client;
        }
        synchronized (this) {
            if (webClient == null) {
                webClient = WebClient.builder()
                        .baseUrl(aiProperties.getOpenai().getBaseUrl())
                        .defaultHeader("Authorization", "Bearer " + aiProperties.getOpenai().getApiKey())
                        .clientConnector(new org.springframework.http.client.reactive.ReactorClientHttpConnector(
                                reactor.netty.http.client.HttpClient.create()
                                        .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                                        .responseTimeout(java.time.Duration.ofSeconds(120))))
                        .build();
            }
            return webClient;
        }
    }

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        return doChat(request, false);
    }

    private AiChatResponse doChat(AiChatRequest request, boolean jsonMode) {
        long start = System.currentTimeMillis();
        Duration timeout = request.getTimeout() != null ? request.getTimeout() : Duration.ofSeconds(120);
        int retry = request.getRetry() != null ? request.getRetry() : 2;
        try {
            Map<String, Object> body = buildRequestBody(request, jsonMode);
            String raw = getClient().post()
                    .uri("/v1/chat/completions")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    // 5xx/429/408/网络超时 为瞬时故障可重试，4xx 鉴权/参数错误直接失败
                    .retryWhen(Retry.backoff(retry, Duration.ofSeconds(1))
                            .filter(e -> {
                                if (e instanceof org.springframework.web.reactive.function.client.WebClientResponseException w) {
                                    return w.getStatusCode().is5xxServerError()
                                            || w.getStatusCode().value() == 429
                                            || w.getStatusCode().value() == 408;
                                }
                                return e instanceof WebClientRequestException;
                            }))
                    .block(timeout);

            return parseResponse(raw, start);
        } catch (WebClientRequestException e) {
            throw new BusinessException(ResultCode.AI_MODEL_CALL_FAILED,
                    "OpenAI 调用失败: " + e.getMessage());
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            throw new BusinessException(ResultCode.AI_MODEL_CALL_FAILED,
                    "OpenAI 调用失败: HTTP " + e.getStatusCode().value());
        } catch (RuntimeException e) {
            throw new BusinessException(ResultCode.AI_MODEL_CALL_FAILED,
                    "OpenAI 调用失败: " + e.getMessage());
        }
    }

    @Override
    public Flux<String> stream(AiChatRequest request) {
        Map<String, Object> body = buildRequestBody(request, false);
        body.put("stream", true);
        Duration timeout = request.getTimeout() != null ? request.getTimeout() : Duration.ofSeconds(120);
        // 流式链路不重试：retryWhen 位于产出 delta 之后，重订阅会从头重放已生成内容导致重复输出；
        // 首字节前失败直接报错，由上层提示用户重试
        // 跨 chunk 行缓冲：网络 chunk 边界不等于 SSE 行边界，需拼接残行再按行解析
        AtomicReference<String> pending = new AtomicReference<>("");
        return getClient().post()
                .uri("/v1/chat/completions")
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(String.class)
                .concatMapIterable(chunk -> parseStreamChunk(chunk, pending))
                .timeout(timeout);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T chatStructured(AiChatRequest request, Class<T> responseType) {
        // 结构化输出启用 json_object 模式，保证返回合法 JSON
        AiChatResponse response = doChat(request, true);
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
        return modelName != null && (modelName.startsWith("gpt-") || modelName.startsWith("o1") ||
                modelName.startsWith("o3") || modelName.startsWith("o4"));
    }

    @Override
    public String getProviderName() {
        return "openai";
    }

    private Map<String, Object> buildRequestBody(AiChatRequest request, boolean jsonMode) {
        List<Map<String, Object>> messages;
        if (request.getMessages() != null && !request.getMessages().isEmpty()) {
            messages = request.getMessages().stream()
                    .map(this::buildMessage)
                    .toList();
        } else {
            messages = List.of(
                    buildTextMessage("system", request.getSystemPrompt()),
                    buildTextMessage("user", request.getUserPrompt())
            );
        }

        Map<String, Object> body = new java.util.HashMap<>();
        body.put("model", request.getModel());
        body.put("messages", messages);
        body.put("temperature", request.getTemperature() != null ? request.getTemperature() : 0.7);
        if (request.getMaxTokens() != null) {
            body.put("max_tokens", request.getMaxTokens());
        }
        if (jsonMode) {
            body.put("response_format", Map.of("type", "json_object"));
        }
        return body;
    }

    private Map<String, Object> buildTextMessage(String role, String content) {
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("role", role);
        m.put("content", content != null ? content : "");
        return m;
    }

    /**
     * 构建消息：支持多模态（contents 非空时按 content 数组序列化），否则用纯文本。
     */
    private Map<String, Object> buildMessage(AiChatRequest.Message message) {
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("role", message.getRole());
        if (message.getContents() != null && !message.getContents().isEmpty()) {
            List<Map<String, Object>> parts = new java.util.ArrayList<>();
            for (AiChatRequest.ContentPart part : message.getContents()) {
                Map<String, Object> p = new java.util.HashMap<>();
                p.put("type", part.getType());
                if ("text".equals(part.getType())) {
                    p.put("text", part.getText());
                } else if ("image_url".equals(part.getType())) {
                    p.put("image_url", Map.of("url", part.getImageUrl() == null ? "" : part.getImageUrl()));
                }
                parts.add(p);
            }
            m.put("content", parts);
        } else {
            m.put("content", message.getContent() != null ? message.getContent() : "");
        }
        return m;
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
                response.setPromptTokens(usage.get("prompt_tokens").asInt());
                response.setCompletionTokens(usage.get("completion_tokens").asInt());
                response.setTotalTokens(usage.get("total_tokens").asInt());
            }
            response.setSuccess(true);
        } catch (Exception e) {
            log.error("Failed to parse OpenAI response: {}", raw, e);
            response.setSuccess(false);
            response.setErrorMsg("响应解析失败: " + e.getMessage());
        }
        response.setLatencyMs(System.currentTimeMillis() - start);
        return response;
    }

    private List<String> parseStreamChunk(String chunk, AtomicReference<String> pending) {
        String buffer = pending.get() + chunk;
        int lastNewline = buffer.lastIndexOf('\n');
        if (lastNewline < 0) {
            pending.set(buffer);
            return List.of();
        }
        String complete = buffer.substring(0, lastNewline + 1);
        pending.set(buffer.substring(lastNewline + 1));
        return Arrays.stream(complete.split("\\r?\\n"))
                .map(String::trim)
                .filter(line -> line.startsWith("data:"))
                .map(line -> line.substring(5).trim())
                .filter(data -> !data.isEmpty() && !"[DONE]".equals(data))
                .map(this::parseDelta)
                .filter(content -> !content.isEmpty())
                .toList();
    }

    private String parseDelta(String data) {
        try {
            JsonNode root = objectMapper.readTree(data);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                return choices.get(0).path("delta").path("content").asText("");
            }
        } catch (JsonProcessingException e) {
            log.debug("Ignore malformed OpenAI stream chunk: {}", data);
        }
        return "";
    }
}
