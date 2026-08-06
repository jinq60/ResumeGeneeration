package com.resume.resume.share.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分享状态响应。
 */
@Data
public class ShareResponse {

    private String token;
    private String url;
    private String status;
    private Boolean hideContact;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
