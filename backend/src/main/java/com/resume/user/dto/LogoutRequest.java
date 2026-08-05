package com.resume.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登出请求：携带要吊销的刷新令牌。
 */
@Data
public class LogoutRequest {

    @NotBlank(message = "刷新令牌不能为空。")
    private String refreshToken;
}
