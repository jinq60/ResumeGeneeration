package com.resume.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码请求。
 */
@Data
public class ChangePasswordRequest {

    @NotBlank(message = "当前密码为必填项。")
    private String oldPassword;

    @NotBlank(message = "新密码为必填项。")
    @Size(min = 8, max = 32, message = "新密码长度应为 8–32 位。")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "新密码需同时包含字母和数字。")
    private String newPassword;
}
