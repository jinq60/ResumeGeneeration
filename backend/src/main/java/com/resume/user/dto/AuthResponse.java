package com.resume.user.dto;

import lombok.Data;

/**
 * 认证响应。
 */
@Data
public class AuthResponse {

    private String userId;
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private Boolean isGuest;
}
