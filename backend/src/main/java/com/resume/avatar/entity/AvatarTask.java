package com.resume.avatar.entity;

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
 * 头像优化任务实体。
 */
@Data
@TableName(value = "avatar_task", autoResultMap = true)
public class AvatarTask {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String userId;
    private String resumeId;
    private String sourceImageUrl;
    private String resultImageUrl;
    private String backgroundType;
    private String style;

    /**
     * 优化选项：keepIdentity / enhanceQuality / removeBackground / brightenSkin 等布尔键值对。
     * data-model-and-ddl.md §4.4 要求使用 TypeHandler 强类型映射，禁止手动 JSON 序列化。
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> options;

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
