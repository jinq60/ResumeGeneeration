package com.resume.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/**
 * 创建 / 更新 AI 规则请求。
 */
@Data
public class AiRuleRequest {

    @NotBlank(message = "规则名称不能为空")
    @Size(max = 128, message = "规则名称最长 128 字符")
    private String name;

    @NotBlank(message = "规则类型不能为空")
    @Size(max = 32, message = "规则类型不合法")
    private String ruleType;

    @Size(max = 512, message = "规则说明最长 512 字符")
    private String description;

    @Size(max = 20000, message = "系统提示词过长")
    private String systemPrompt;

    @Size(max = 20000, message = "用户提示词过长")
    private String userPrompt;

    private Map<String, Object> params;
}