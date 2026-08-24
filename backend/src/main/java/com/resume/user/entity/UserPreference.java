package com.resume.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 用户偏好设置实体。
 */
@Data
@TableName(value = "user_preference", autoResultMap = true)
public class UserPreference {

    @TableId(type = IdType.INPUT)
    private String userId;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> preferences;

    private LocalDateTime updatedAt;
}