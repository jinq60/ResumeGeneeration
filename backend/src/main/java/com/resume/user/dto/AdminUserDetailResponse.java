package com.resume.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台用户详情响应。
 */
@Data
public class AdminUserDetailResponse {

    private String userId;
    private String nickname;
    private String phone;
    private String email;
    private String avatarUrl;
    private String role;
    private String status;
    private Boolean isGuest;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}