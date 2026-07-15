package com.resume.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 调用日志，记录每次 LLM 调用的 token 消耗、耗时与成本。
 */
@Data
@TableName("ai_call_log")
public class AiCallLog {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String userId;

    private String featureKey;

    private String providerName;

    private String modelName;

    private String requestHash;

    private Integer promptTokens;

    private Integer completionTokens;

    private Integer totalTokens;

    private long latencyMs;

    private boolean success;

    private String errorMsg;

    private LocalDateTime createdAt;
}
