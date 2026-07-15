package com.resume.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 刷新 Token 请求。
 */
@Data
public class RefreshRequest {

    @NotBlank(message = "刷新令牌为必填项。")
    private String refreshToken;
}
