package com.resume.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 每日配额计数实体。
 */
@Data
@TableName("ai_daily_quota")
public class AiDailyQuota {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String userId;
    private String featureKey;
    private String quotaDate;
    private Integer usedCount;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
