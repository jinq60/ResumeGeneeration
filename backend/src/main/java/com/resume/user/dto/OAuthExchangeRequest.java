package com.resume.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * OAuth 一次性授权码换取令牌请求。
 */
@Data
public class OAuthExchangeRequest {

    @NotBlank(message = "授权码不能为空")
    @Size(max = 128, message = "授权码格式不正确")
    private String code;
}
