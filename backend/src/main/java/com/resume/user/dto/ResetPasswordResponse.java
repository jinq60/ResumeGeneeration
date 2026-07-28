package com.resume.user.dto;

import lombok.Data;

/**
 * 重置用户密码响应。
 * <p>
 * 仅在管理员重置密码时返回服务端生成的一次性临时密码，用户首次登录后应自行修改。
 * </p>
 */
@Data
public class ResetPasswordResponse {

    private String userId;
    /**
     * 一次性临时密码（明文，仅此一次返回）。
     */
    private String temporaryPassword;
}