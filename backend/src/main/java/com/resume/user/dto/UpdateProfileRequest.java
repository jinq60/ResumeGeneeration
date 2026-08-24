package com.resume.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新当前用户资料请求。
 */
@Data
public class UpdateProfileRequest {

    @Size(max = 50, message = "昵称最长 50 字符")
    private String nickname;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱过长")
    private String email;

    @Size(max = 512, message = "头像 URL 过长")
    private String avatarUrl;
}