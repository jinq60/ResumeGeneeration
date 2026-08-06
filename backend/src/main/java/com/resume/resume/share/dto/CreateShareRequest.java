package com.resume.resume.share.dto;

import jakarta.validation.constraints.Future;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建/轮换分享请求。
 */
@Data
public class CreateShareRequest {

    /**
     * 分享页是否隐藏联系方式（手机/邮箱/个人链接）。
     */
    private Boolean hideContact = false;

    /**
     * 过期时间；为空表示永久有效，必须晚于当前时间。
     */
    @Future(message = "过期时间必须晚于当前时间。")
    private LocalDateTime expiresAt;
}
