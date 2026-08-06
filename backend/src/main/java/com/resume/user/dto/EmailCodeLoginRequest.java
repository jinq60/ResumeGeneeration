package com.resume.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 邮箱验证码登录请求。
 */
@Data
public class EmailCodeLoginRequest {

    @NotBlank(message = "邮箱不能为空。")
    @Email(message = "邮箱格式不正确。")
    private String email;

    @NotBlank(message = "验证码不能为空。")
    @Pattern(regexp = "^\\d{6}$", message = "验证码为 6 位数字。")
    private String code;
}
