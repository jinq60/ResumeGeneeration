package com.resume.ai.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Prompt 模板管理器，从配置中读取模板并做变量替换。
 */
@Component
@RequiredArgsConstructor
public class AiPromptTemplates {

    private final AiProperties aiProperties;

    public String get(String promptKey) {
        Map<String, String> prompts = aiProperties.getPrompts();
        if (prompts == null || !prompts.containsKey(promptKey)) {
            throw new IllegalArgumentException("Prompt template not found: " + promptKey);
        }
        return prompts.get(promptKey);
    }

    public String render(String promptKey, Map<String, String> variables) {
        String template = get(promptKey);
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}
