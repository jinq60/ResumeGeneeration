package com.resume.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * AI 规则实体（提示词 / 评分规则 / 敏感词库等）。
 */
@Data
@TableName(value = "ai_rule", autoResultMap = true)
public class AiRule {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 版本族 ID：同一规则的所有版本共享，首个版本为自身 ID。 */
    private String familyId;

    private String name;
    private String ruleType;
    private String description;
    private String systemPrompt;
    private String userPrompt;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> params;

    private String status;
    private Integer version;
    private LocalDateTime publishedAt;
    private String createdBy;

    /**
     * 逻辑删除标记：0 未删除，1 已删除。
     */
    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}