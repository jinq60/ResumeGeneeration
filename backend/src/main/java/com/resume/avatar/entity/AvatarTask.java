package com.resume.avatar.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 头像优化任务实体。
 */
@Data
@TableName("avatar_task")
public class AvatarTask {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String userId;
    private String resumeId;
    private String sourceImageUrl;
    private String resultImageUrl;
    private String backgroundType;
    private String style;

    private String options;

    private String status;
    private String errorMsg;
    private LocalDateTime completedAt;

    /**
     * 逻辑删除标记：0 未删除，1 已删除。
     */
    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
