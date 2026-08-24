package com.resume.notification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户通知实体。
 */
@Data
@TableName(value = "notification")
public class Notification {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String userId;
    private String type;
    private String title;
    private String content;
    private Integer readFlag;

    /**
     * 逻辑删除标记：0 未删除，1 已删除。
     */
    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}