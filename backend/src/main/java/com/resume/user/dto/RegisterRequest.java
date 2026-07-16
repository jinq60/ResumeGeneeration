package com.resume.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 账号注册请求。
 */
@Data
public class RegisterRequest {

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确。")
    private String phone;

    @jakarta.validation.constraints.Email(message = "邮箱格式不正确。")
    @Size(max = 128, message = "邮箱过长。")
    private String email;

    @NotBlank(message = "验证码为必填项。")
    @Pattern(regexp = "^\\d{6}$", message = "验证码应为 6 位数字。")
    private String verifyCode;

    @NotBlank(message = "密码为必填项。")
    @Size(min = 8, max = 32, message = "密码长度应为 8–32 位。")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "密码需同时包含字母和数字。")
    private String password;
}
