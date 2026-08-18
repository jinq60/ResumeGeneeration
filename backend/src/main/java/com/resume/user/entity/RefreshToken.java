package com.resume.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 刷新令牌落库记录。
 * <p>
 * 用户在登录 / 注册 / 游客会话创建 / 刷新时插入新的 refresh token 哈希，
 * 旧令牌在下次刷新时删除。校验刷新请求时，除 JWT 自身签名校验外，
 * 还会校验对应 token_hash 在库中存在且未过期，从而支持服务端主动吊销。
 * </p>
 */
@Data
@TableName("refresh_token")
public class RefreshToken {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String userId;

    /**
     * refresh token 的 SHA-256 哈希，仅存哈希避免泄露。
     */
    private String tokenHash;

    private LocalDateTime expiresAt;

    private LocalDateTime createdAt;
}