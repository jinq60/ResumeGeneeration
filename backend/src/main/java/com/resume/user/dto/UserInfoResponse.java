package com.resume.user.dto;

import lombok.Data;

/**
 * 当前用户信息响应。
 */
@Data
public class UserInfoResponse {

    private String userId;
    private String nickname;
    private String phone;
    private String email;
    private String avatarUrl;
    private Boolean isGuest;
}
