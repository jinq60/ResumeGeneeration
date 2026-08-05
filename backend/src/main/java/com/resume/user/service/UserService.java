package com.resume.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.resume.common.constant.BizConstant;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
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

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 32;
    private static final java.util.regex.Pattern PASSWORD_COMPLEXITY =
            java.util.regex.Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).+$");

    /**
     * 注册账号。
     */
    @Transactional(rollbackFor = Exception.class)
    public AuthResponse register(RegisterRequest request) {
        log.info("register start: phone={}, email={}", request.getPhone(),
                maskEmail(request.getEmail()));
        if (StringUtils.isBlank(request.getPhone()) && StringUtils.isBlank(request.getEmail())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "手机号与邮箱至少填写一个。");
        }
        if (StringUtils.isBlank(request.getPassword())
                || request.getPassword().length() < MIN_PASSWORD_LENGTH
                || request.getPassword().length() > MAX_PASSWORD_LENGTH
                || !PASSWORD_COMPLEXITY.matcher(request.getPassword()).matches()) {
            throw new BusinessException(ResultCode.AUTH_PASSWORD_TOO_WEAK,
                    "密码长度应为 " + MIN_PASSWORD_LENGTH + "–" + MAX_PASSWORD_LENGTH + " 位，且需同时包含字母和数字。");
        }

        // P0：验证码占位校验，任意 6 位数字均通过。
        // 防御性校验：即使 DTO 校验被绕过，也不应在此抛 NPE（StringUtils.isBlank 已兼容 null）。
        if (StringUtils.isBlank(request.getVerifyCode()) || !request.getVerifyCode().matches("^\\d{6}$")) {
            throw new BusinessException(ResultCode.AUTH_VERIFY_CODE_INVALID, "验证码不正确或已过期。");
        }

        if (StringUtils.isNotBlank(request.getPhone())) {
            User exist = findByPhoneIncludingDeleted(request.getPhone());
            if (exist != null && BizConstant.NOT_DELETED.equals(exist.getDeleted())) {
                throw new BusinessException(ResultCode.AUTH_PHONE_REGISTERED, "该手机号已注册。");
            }
            if (exist != null) {
                // 回收被逻辑删除账号占用的唯一索引，允许重新注册
                releaseUserIdentity(exist.getId(), request.getPhone(), null);
            }
        }
        if (StringUtils.isNotBlank(request.getEmail())) {
            User exist = findByEmailIncludingDeleted(request.getEmail());
            if (exist != null && BizConstant.NOT_DELETED.equals(exist.getDeleted())) {
                throw new BusinessException(ResultCode.AUTH_EMAIL_REGISTERED, "该邮箱已注册。");
            }
            if (exist != null) {
                releaseUserIdentity(exist.getId(), null, request.getEmail());
            }
        }

        User user = new User();
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setIsGuest(BizConstant.IS_NOT_GUEST);
        user.setRole(BizConstant.USER_ROLE_USER);
        user.setStatus(BizConstant.USER_STATUS_ACTIVE);
        user.setDeleted(BizConstant.NOT_DELETED);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);

        log.info("register success: userId={}, phone={}", user.getId(), maskPhone(user.getPhone()));
        return buildAuthResponse(user);
    }

    /**
     * 登录。当 loginType 未传时自动识别账号类型。
     */
    public AuthResponse login(LoginRequest request) {
        log.info("login start: account={}", maskAccount(request.getAccount()));
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

        if (user == null || BizConstant.IS_GUEST.equals(user.getIsGuest())) {
            throw new BusinessException(ResultCode.AUTH_ACCOUNT_NOT_FOUND, "账号不存在。");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.AUTH_PASSWORD_INCORRECT, "密码不正确。");
        }
        if (BizConstant.USER_STATUS_DISABLED.equals(user.getStatus())) {
            throw new BusinessException(ResultCode.AUTH_ACCOUNT_LOCKED, "账号已被锁定，请稍后再试。");
        }

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
                .eq(RefreshToken::getTokenHash, tokenHash)
                .eq(RefreshToken::getDeleted, BizConstant.NOT_DELETED);
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

    private AuthResponse buildAuthResponse(User user) {
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
        record.setDeleted(BizConstant.NOT_DELETED);
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

    /**
     * 查询含逻辑删除记录（唯一索引不区分 deleted，注册查重需包含已删记录）。
     */
    private User findByPhoneIncludingDeleted(String phone) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, phone);
        return userMapper.selectOne(wrapper);
    }

    private User findByEmailIncludingDeleted(String email) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, email);
        return userMapper.selectOne(wrapper);
    }

    /**
     * 释放逻辑删除账号占用的唯一键（phone/email 置 NULL，MySQL 唯一索引允许多个 NULL）。
     */
    private void releaseUserIdentity(String userId, String phone, String email) {
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(User::getId, userId);
        if (phone != null) {
            wrapper.set(User::getPhone, null);
        }
        if (email != null) {
            wrapper.set(User::getEmail, null);
        }
        wrapper.set(User::getUpdatedAt, LocalDateTime.now());
        userMapper.update(null, wrapper);
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
