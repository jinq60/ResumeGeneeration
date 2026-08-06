package com.resume.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 多方式认证绑定实体（第三方登录 / 验证码登录凭据）。
 */
@Data
@TableName("user_auth")
public class UserAuth {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String userId;
    private String provider;
    private String account;
    private String credential;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
