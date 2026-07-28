package com.resume.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 更新用户状态请求。
 */
@Data
public class UpdateUserStatusRequest {

    @NotBlank(message = "状态为必填项。")
    @Pattern(regexp = "^(active|disabled)$", message = "状态仅支持 active 或 disabled。")
    private String status;
}