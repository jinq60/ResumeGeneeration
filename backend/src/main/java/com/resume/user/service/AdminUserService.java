package com.resume.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.resume.common.constant.BizConstant;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.common.service.AuditLogService;
import com.resume.user.dto.AdminUserDetailResponse;
import com.resume.user.dto.AdminUserListItemResponse;
import com.resume.user.dto.ResetPasswordResponse;
import com.resume.user.dto.UserStatsResponse;
import com.resume.user.entity.RefreshToken;
import com.resume.user.entity.User;
import com.resume.user.mapper.RefreshTokenMapper;
import com.resume.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台用户管理业务服务。
 * <p>
 * 仅供 ADMIN 角色调用，区别于 {@link UserService} 的终端用户自助服务。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenMapper refreshTokenMapper;
    private final AuditLogService auditLogService;

    /**
     * 默认临时密码长度，可在配置中覆盖。
     */
    @Value("${app.admin.reset-password.length:12}")
    private int tempPasswordLength;

    private static final String TEMP_PASSWORD_CHARS =
            "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";

    /**
     * 后台用户列表。
     */
    public Page<AdminUserListItemResponse> listUsers(int page, int size, String status, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getDeleted, BizConstant.NOT_DELETED);
        if (StringUtils.isNotBlank(status)) {
            wrapper.eq(User::getStatus, status);
        }
        if (StringUtils.isNotBlank(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(User::getNickname, kw)
                    .or().like(User::getPhone, kw)
                    .or().like(User::getEmail, kw));
        }
        wrapper.orderByDesc(User::getCreatedAt);

        Page<User> pageParam = new Page<>(page, size);
        Page<User> result = userMapper.selectPage(pageParam, wrapper);

        List<AdminUserListItemResponse> list = result.getRecords().stream()
                .map(this::toListItemResponse)
                .toList();
        Page<AdminUserListItemResponse> responsePage = new Page<>();
        responsePage.setRecords(list);
        responsePage.setTotal(result.getTotal());
        responsePage.setCurrent(result.getCurrent());
        responsePage.setSize(result.getSize());
        responsePage.setPages(result.getPages());
        return responsePage;
    }

    /**
     * 获取用户详情。
     */
    public AdminUserDetailResponse getUser(String userId) {
        User user = findUserById(userId);
        return toDetailResponse(user);
    }

    /**
     * 后台用户统计。
     */
    public UserStatsResponse stats() {
        UserStatsResponse response = new UserStatsResponse();

        LambdaQueryWrapper<User> notDeleted = new LambdaQueryWrapper<>();
        notDeleted.eq(User::getDeleted, BizConstant.NOT_DELETED);
        response.setTotalUsers(userMapper.selectCount(notDeleted));

        LambdaQueryWrapper<User> active = new LambdaQueryWrapper<>();
        active.eq(User::getDeleted, BizConstant.NOT_DELETED)
                .eq(User::getStatus, BizConstant.USER_STATUS_ACTIVE);
        response.setActiveUsers(userMapper.selectCount(active));

        LambdaQueryWrapper<User> disabled = new LambdaQueryWrapper<>();
        disabled.eq(User::getDeleted, BizConstant.NOT_DELETED)
                .eq(User::getStatus, BizConstant.USER_STATUS_DISABLED);
        response.setDisabledUsers(userMapper.selectCount(disabled));

        LambdaQueryWrapper<User> guest = new LambdaQueryWrapper<>();
        guest.eq(User::getDeleted, BizConstant.NOT_DELETED)
                .eq(User::getIsGuest, BizConstant.IS_GUEST);
        response.setGuestUsers(userMapper.selectCount(guest));

        LambdaQueryWrapper<User> registered = new LambdaQueryWrapper<>();
        registered.eq(User::getDeleted, BizConstant.NOT_DELETED)
                .eq(User::getIsGuest, BizConstant.IS_NOT_GUEST);
        response.setRegisteredUsers(userMapper.selectCount(registered));

        LambdaQueryWrapper<User> admin = new LambdaQueryWrapper<>();
        admin.eq(User::getDeleted, BizConstant.NOT_DELETED)
                .eq(User::getRole, BizConstant.USER_ROLE_ADMIN);
        response.setAdminUsers(userMapper.selectCount(admin));

        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        LambdaQueryWrapper<User> todayNew = new LambdaQueryWrapper<>();
        todayNew.eq(User::getDeleted, BizConstant.NOT_DELETED)
                .ge(User::getCreatedAt, todayStart);
        response.setTodayNewUsers(userMapper.selectCount(todayNew));

        return response;
    }

    /**
     * 更新用户状态（启用/禁用）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(String userId, String status, String operatorId) {
        User user = findUserById(userId);
        if (!BizConstant.USER_STATUS_ACTIVE.equals(status)
                && !BizConstant.USER_STATUS_DISABLED.equals(status)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "用户状态仅支持 active 或 disabled。");
        }
        if (operatorId != null && operatorId.equals(userId)) {
            throw new BusinessException(ResultCode.ACCESS_DENIED, "不能对自己的账号执行此操作。");
        }
        assertNotLastAdmin(user);
        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        if (BizConstant.USER_STATUS_DISABLED.equals(status)) {
            revokeUserRefreshTokens(userId);
        }
        auditLogService.record(operatorId, "admin_update_status", userId, "status=" + status);
    }

    /**
     * 设置/取消管理员角色。
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(String userId, String role, String operatorId) {
        User user = findUserById(userId);
        if (!BizConstant.USER_ROLE_USER.equals(role) && !BizConstant.USER_ROLE_ADMIN.equals(role)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "角色仅支持 USER 或 ADMIN。");
        }
        if (operatorId != null && operatorId.equals(userId)) {
            throw new BusinessException(ResultCode.ACCESS_DENIED, "不能修改自己的角色。");
        }
        if (BizConstant.USER_ROLE_ADMIN.equals(user.getRole()) && BizConstant.USER_ROLE_USER.equals(role)) {
            assertNotLastAdmin(user);
        }
        user.setRole(role);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        if (BizConstant.USER_ROLE_USER.equals(role)) {
            revokeUserRefreshTokens(userId);
        }
        auditLogService.record(operatorId, "admin_update_role", userId, "role=" + role);
    }

    /**
     * 管理员重置用户密码：生成一次性临时密码，BCrypt 哈希入库，并吊销该用户全部刷新令牌。
     */
    @Transactional(rollbackFor = Exception.class)
    public ResetPasswordResponse resetPassword(String userId, String operatorId) {
        User user = findUserById(userId);
        if (BizConstant.IS_GUEST.equals(user.getIsGuest())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "游客账号无需重置密码。");
        }

        String tempPassword = generateTemporaryPassword();
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        // 旧会话全部失效：吊销该用户所有刷新令牌
        revokeUserRefreshTokens(userId);
        auditLogService.record(operatorId, "admin_reset_password", userId, null);

        log.info("Admin reset password for userId={}", userId);

        ResetPasswordResponse response = new ResetPasswordResponse();
        response.setUserId(userId);
        response.setTemporaryPassword(tempPassword);
        return response;
    }

    /**
     * 后台逻辑删除用户（同步禁用账号、吊销令牌，防止再登录）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(String userId, String operatorId) {
        User user = findUserById(userId);
        if (operatorId != null && operatorId.equals(userId)) {
            throw new BusinessException(ResultCode.ACCESS_DENIED, "不能删除自己的账号。");
        }
        assertNotLastAdmin(user);
        User update = new User();
        update.setId(user.getId());
        update.setStatus(BizConstant.USER_STATUS_DISABLED);
        update.setDeleted(BizConstant.DELETED);
        update.setUpdatedAt(LocalDateTime.now());
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(User::getId, userId);
        userMapper.update(update, wrapper);
        revokeUserRefreshTokens(userId);
        auditLogService.record(operatorId, "admin_delete_user", userId, null);
    }

    /**
     * 防止移除最后一个 ADMIN，导致系统失去管理员。
     */
    private void assertNotLastAdmin(User user) {
        if (!BizConstant.USER_ROLE_ADMIN.equals(user.getRole())) {
            return;
        }
        LambdaQueryWrapper<User> adminCount = new LambdaQueryWrapper<>();
        adminCount.eq(User::getDeleted, BizConstant.NOT_DELETED)
                .eq(User::getRole, BizConstant.USER_ROLE_ADMIN);
        if (userMapper.selectCount(adminCount) <= 1) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "系统至少需要保留一名管理员。");
        }
    }

    /**
     * 吊销指定用户的全部刷新令牌。
     */
    private void revokeUserRefreshTokens(String userId) {
        LambdaUpdateWrapper<com.resume.user.entity.RefreshToken> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(com.resume.user.entity.RefreshToken::getUserId, userId);
        refreshTokenMapper.delete(wrapper);
    }

    private User findUserById(String userId) {
        User user = userMapper.selectById(userId);
        if (user == null || BizConstant.DELETED.equals(user.getDeleted())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "用户不存在。");
        }
        return user;
    }

    private AdminUserListItemResponse toListItemResponse(User user) {
        AdminUserListItemResponse response = new AdminUserListItemResponse();
        response.setUserId(user.getId());
        response.setNickname(user.getNickname());
        response.setPhone(maskPhone(user.getPhone()));
        response.setEmail(maskEmail(user.getEmail()));
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());
        response.setIsGuest(BizConstant.IS_GUEST.equals(user.getIsGuest()));
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }

    private AdminUserDetailResponse toDetailResponse(User user) {
        AdminUserDetailResponse response = new AdminUserDetailResponse();
        response.setUserId(user.getId());
        response.setNickname(user.getNickname());
        response.setPhone(maskPhone(user.getPhone()));
        response.setEmail(maskEmail(user.getEmail()));
        response.setAvatarUrl(user.getAvatarUrl());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());
        response.setIsGuest(BizConstant.IS_GUEST.equals(user.getIsGuest()));
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }

    private String generateTemporaryPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(tempPasswordLength);
        for (int i = 0; i < tempPasswordLength; i++) {
            sb.append(TEMP_PASSWORD_CHARS.charAt(random.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }

    private String maskPhone(String phone) {
        if (StringUtils.isBlank(phone) || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    private String maskEmail(String email) {
        if (StringUtils.isBlank(email) || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String local = parts[0];
        String mask = local.length() <= 2 ? "*" : local.charAt(0) + "***";
        return mask + "@" + parts[1];
    }
}