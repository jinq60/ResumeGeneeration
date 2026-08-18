package com.resume.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.resume.common.constant.BizConstant;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.common.service.AuditLogService;
import com.resume.user.dto.*;
import com.resume.user.entity.RefreshToken;
import com.resume.user.entity.User;
import com.resume.user.mapper.RefreshTokenMapper;
import com.resume.user.mapper.UserMapper;
import com.resume.user.security.JwtTokenProvider;
import com.resume.user.security.TokenHashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * 用户与认证业务服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptGuard loginAttemptGuard;
    private final AuditLogService auditLogService;

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 32;
    private static final Pattern PASSWORD_COMPLEXITY =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).+$");

    /**
     * 登录。当 loginType 未传时自动识别账号类型。
     * <p>
     * 邮箱账号不存在时自动创建（登录即注册）；手机号不存在时保持报错。
     * 连续失败达到上限后账号临时锁定（LoginAttemptGuard），防止密码爆破。
     * </p>
     */
    public AuthResponse login(LoginRequest request) {
        log.info("login start: account={}", maskAccount(request.getAccount()));
        if (loginAttemptGuard.isLocked(request.getAccount())) {
            throw new BusinessException(ResultCode.AUTH_ACCOUNT_LOCKED,
                    "登录失败次数过多，账号已临时锁定，请稍后再试。");
        }
        String loginType = request.getLoginType();
        if (StringUtils.isBlank(loginType)) {
            loginType = detectLoginType(request.getAccount());
        }

        User user;
        if ("phone".equals(loginType)) {
            user = findByPhone(request.getAccount());
        } else {
            user = findByEmail(request.getAccount());
        }

        if (user == null) {
            if (!"email".equals(loginType)) {
                throw new BusinessException(ResultCode.AUTH_ACCOUNT_NOT_FOUND, "账号不存在。");
            }
            // 邮箱不存在时不再"登录即注册"（存在账号抢占风险），
            // 引导用户走邮箱验证码登录，由验证码校验邮箱所有权后再自动创建账号
            throw new BusinessException(ResultCode.AUTH_ACCOUNT_NOT_FOUND,
                    "该邮箱尚未注册，请使用邮箱验证码登录。");
        } else if (BizConstant.IS_GUEST.equals(user.getIsGuest())) {
            throw new BusinessException(ResultCode.AUTH_ACCOUNT_NOT_FOUND, "账号不存在。");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            loginAttemptGuard.recordFailure(request.getAccount());
            throw new BusinessException(ResultCode.AUTH_PASSWORD_INCORRECT, "密码不正确。");
        }
        if (BizConstant.USER_STATUS_DISABLED.equals(user.getStatus())) {
            throw new BusinessException(ResultCode.AUTH_ACCOUNT_LOCKED, "账号已被锁定，请稍后再试。");
        }

        loginAttemptGuard.reset(request.getAccount());
        auditLogService.record(user.getId(), "login", user.getId(), "loginType=" + loginType);
        log.info("login success: userId={}", user.getId());
        return buildAuthResponse(user);
    }

    /**
     * 创建游客会话。
     */
    @Transactional(rollbackFor = Exception.class)
    public AuthResponse createGuest() {
        User user = new User();
        user.setIsGuest(BizConstant.IS_GUEST);
        user.setRole(BizConstant.USER_ROLE_USER);
        user.setStatus(BizConstant.USER_STATUS_ACTIVE);
        user.setDeleted(BizConstant.NOT_DELETED);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);

        log.info("guest session created: userId={}", user.getId());
        auditLogService.record(user.getId(), "guest_create", user.getId(), null);
        return buildAuthResponse(user);
    }

    /**
     * 刷新 Token。
     * <p>
     * 校验顺序：
     * <ol>
     *   <li>JWT 签名 + 类型 {@code refresh} 有效；</li>
     *   <li>计算 SHA-256 哈希后查库，确认该 refresh token 已落库且未过期、未删除；</li>
     *   <li>原子删除当前 refresh 记录（先删后验，一次性使用，防并发复用）；</li>
     *   <li>用户存在、未逻辑删除、未被禁用；</li>
     *   <li>颁发新的 access + refresh 对。</li>
     * </ol>
     * </p>
     */
    @Transactional(rollbackFor = Exception.class)
    public AuthResponse refresh(RefreshRequest request) {
        if (!jwtTokenProvider.validateRefreshToken(request.getRefreshToken())) {
            throw new BusinessException(ResultCode.AUTH_REFRESH_TOKEN_INVALID, "刷新令牌无效、已过期或类型不正确。");
        }
        String userId = jwtTokenProvider.getUserId(request.getRefreshToken());
        String tokenHash = TokenHashUtil.hash(request.getRefreshToken());

        LambdaQueryWrapper<RefreshToken> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RefreshToken::getUserId, userId)
                .eq(RefreshToken::getTokenHash, tokenHash);
        RefreshToken stored = refreshTokenMapper.selectOne(wrapper);
        if (stored == null || stored.getExpiresAt() == null
                || stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.AUTH_REFRESH_TOKEN_INVALID, "刷新令牌无效或已过期。");
        }

        // refresh token rotation：先删后验。并发复用同一令牌时只有第一个请求能删除成功，
        // 后续请求影响 0 行即判定为复用攻击直接拒绝。
        int removed = refreshTokenMapper.delete(wrapper);
        if (removed == 0) {
            throw new BusinessException(ResultCode.AUTH_REFRESH_TOKEN_INVALID, "刷新令牌已失效，请重新登录。");
        }

        User user = userMapper.selectById(userId);
        if (user == null || BizConstant.DELETED.equals(user.getDeleted())) {
            throw new BusinessException(ResultCode.AUTH_REFRESH_TOKEN_INVALID, "刷新令牌无效或已过期。");
        }
        if (BizConstant.USER_STATUS_DISABLED.equals(user.getStatus())) {
            throw new BusinessException(ResultCode.AUTH_ACCOUNT_LOCKED, "账号已被锁定，无法刷新令牌。");
        }
        return buildAuthResponse(user);
    }

    /**
     * 登出：吊销指定刷新令牌（按哈希删除），使该会话立即失效。
     */
    @Transactional(rollbackFor = Exception.class)
    public void logout(LogoutRequest request) {
        if (request == null || StringUtils.isBlank(request.getRefreshToken())) {
            return;
        }
        String tokenHash = TokenHashUtil.hash(request.getRefreshToken());
        LambdaUpdateWrapper<RefreshToken> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(RefreshToken::getTokenHash, tokenHash);
        refreshTokenMapper.delete(wrapper);
        auditLogService.record(null, "logout", null, "refresh token revoked");
        log.info("logout: refresh token revoked");
    }

    /**
     * 吊销指定用户全部有效刷新令牌（禁用/删除/重置密码时调用）。
     */
    public void revokeUserRefreshTokens(String userId) {
        if (StringUtils.isBlank(userId)) {
            return;
        }
        LambdaUpdateWrapper<RefreshToken> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(RefreshToken::getUserId, userId);
        refreshTokenMapper.delete(wrapper);
    }

    /**
     * 修改当前用户密码：校验旧密码后更新哈希，并吊销全部刷新令牌使旧会话失效。
     */
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(String userId, ChangePasswordRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null || BizConstant.DELETED.equals(user.getDeleted())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "用户不存在。");
        }
        if (BizConstant.IS_GUEST.equals(user.getIsGuest())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "游客账号无需修改密码。");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.AUTH_PASSWORD_INCORRECT, "当前密码不正确。");
        }
        if (StringUtils.isBlank(request.getNewPassword())
                || request.getNewPassword().length() < MIN_PASSWORD_LENGTH
                || request.getNewPassword().length() > MAX_PASSWORD_LENGTH
                || !PASSWORD_COMPLEXITY.matcher(request.getNewPassword()).matches()) {
            throw new BusinessException(ResultCode.AUTH_PASSWORD_TOO_WEAK,
                    "密码长度应为 " + MIN_PASSWORD_LENGTH + "–" + MAX_PASSWORD_LENGTH + " 位，且需同时包含字母和数字。");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        // 旧会话全部失效：吊销该用户所有刷新令牌
        revokeUserRefreshTokens(userId);
        auditLogService.record(userId, "change_password", userId, null);
        log.info("changePassword success: userId={}", userId);
    }

    /**
     * 获取当前用户信息。
     */
    public UserInfoResponse getCurrentUser(String userId) {
        log.debug("getCurrentUser: userId={}", userId);
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "用户不存在。");
        }
        UserInfoResponse response = new UserInfoResponse();
        response.setUserId(user.getId());
        response.setNickname(user.getNickname());
        response.setPhone(maskPhone(user.getPhone()));
        response.setEmail(maskEmail(user.getEmail()));
        response.setAvatarUrl(user.getAvatarUrl());
        response.setIsGuest(BizConstant.IS_GUEST.equals(user.getIsGuest()));
        return response;
    }

    public AuthResponse buildAuthResponse(User user) {
        AuthResponse response = new AuthResponse();
        response.setUserId(user.getId());
        String role = user.getRole() == null ? BizConstant.USER_ROLE_USER : user.getRole();
        response.setAccessToken(jwtTokenProvider.generateAccessToken(user.getId(),
                BizConstant.IS_GUEST.equals(user.getIsGuest()), role));
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(jwtTokenProvider.getAccessTokenExpiration() / 1000);
        response.setIsGuest(BizConstant.IS_GUEST.equals(user.getIsGuest()));
        storeRefreshToken(user.getId(), refreshToken);
        return response;
    }

    /**
     * 将 refresh token 哈希落库，仅存哈希避免明文泄露。
     */
    private void storeRefreshToken(String userId, String refreshToken) {
        if (StringUtils.isBlank(refreshToken)) {
            return;
        }
        RefreshToken record = new RefreshToken();
        record.setUserId(userId);
        record.setTokenHash(TokenHashUtil.hash(refreshToken));
        record.setExpiresAt(LocalDateTime.now().plus(Duration.ofMillis(jwtTokenProvider.getRefreshTokenExpiration())));
        record.setCreatedAt(LocalDateTime.now());
        refreshTokenMapper.insert(record);
    }

    /**
     * 根据账号格式自动识别登录类型：匹配手机号返回 phone，否则 email。
     */
    private String detectLoginType(String account) {
        if (StringUtils.isBlank(account)) {
            return "email";
        }
        if (account.matches("^1[3-9]\\d{9}$")) {
            return "phone";
        }
        return "email";
    }

    private User findByPhone(String phone) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, phone)
                .eq(User::getDeleted, BizConstant.NOT_DELETED);
        return userMapper.selectOne(wrapper);
    }

    private User findByEmail(String email) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, email)
                .eq(User::getDeleted, BizConstant.NOT_DELETED);
        return userMapper.selectOne(wrapper);
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
        if (local.length() <= 2) {
            return "*@" + parts[1];
        }
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + "@" + parts[1];
    }

    /**
     * 账号脱敏（用于日志中保留可识别但不暴露的标识）。
     */
    private String maskAccount(String account) {
        if (StringUtils.isBlank(account)) {
            return "";
        }
        if (account.matches("^1[3-9]\\d{9}$")) {
            return maskPhone(account);
        }
        return maskEmail(account);
    }
}
