package com.resume.audit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 内容审核记录实体。
 */
@Data
@TableName(value = "content_audit")
public class ContentAudit {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String targetType;
    private String targetId;
    private String targetTitle;
    private String userId;
    private String riskLevel;
    private String status;
    private String reviewerId;
    private String reviewNote;
    private LocalDateTime reviewedAt;

    /**
     * 逻辑删除标记：0 未删除，1 已删除。
     */
    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}