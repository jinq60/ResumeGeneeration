package com.resume.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 账号登录请求。
 */
@Data
public class LoginRequest {

    @NotBlank(message = "账号为必填项。")
    private String account;

    @NotBlank(message = "密码为必填项。")
    private String password;

    @Pattern(regexp = "^(phone|email)$", message = "登录类型不正确。")
    private String loginType;
}
