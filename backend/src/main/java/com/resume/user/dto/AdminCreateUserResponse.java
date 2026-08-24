package com.resume.user.dto;

import lombok.Data;

/**
 * 管理端新增用户响应。
 */
@Data
public class AdminCreateUserResponse {

    private String userId;
    private String nickname;
    private String email;
    private String phone;
    /** 未指定初始密码时返回一次性临时密码（仅本次返回）。 */
    private String temporaryPassword;
    private String message;
}