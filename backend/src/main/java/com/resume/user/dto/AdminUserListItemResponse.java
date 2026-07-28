package com.resume.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台用户列表项响应。
 */
@Data
public class AdminUserListItemResponse {

    private String userId;
    private String nickname;
    private String phone;
    private String email;
    private String role;
    private String status;
    private Boolean isGuest;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}