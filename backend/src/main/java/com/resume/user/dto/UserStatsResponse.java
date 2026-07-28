package com.resume.user.dto;

import lombok.Data;

/**
 * 后台用户统计响应。
 */
@Data
public class UserStatsResponse {

    private Long totalUsers;
    private Long activeUsers;
    private Long disabledUsers;
    private Long guestUsers;
    private Long registeredUsers;
    private Long adminUsers;
    private Long todayNewUsers;
}
