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

    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MAX_PASSWORD_LENGTH = 32;

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
                || request.getPassword().length() > MAX_PASSWORD_LENGTH) {
            throw new BusinessException(ResultCode.AUTH_PASSWORD_TOO_WEAK,
                    "密码长度应为 " + MIN_PASSWORD_LENGTH + "–" + MAX_PASSWORD_LENGTH + " 位。");
        }

        // P0：验证码占位校验，任意 6 位数字均通过。
        // 防御性校验：即使 DTO 校验被绕过，也不应在此抛 NPE。
        String verifyCode = request.getVerifyCode();
        if (verifyCode == null || !verifyCode.matches("^\\d{6}$")) {
            throw new BusinessException(ResultCode.AUTH_VERIFY_CODE_INVALID, "验证码不正确或已过期。");
        }

        if (StringUtils.isNotBlank(request.getPhone())) {
            User exist = findByPhone(request.getPhone());
            if (exist != null) {
                throw new BusinessException(ResultCode.AUTH_PHONE_REGISTERED, "该手机号已注册。");
            }
        }
        if (StringUtils.isNotBlank(request.getEmail())) {
            User exist = findByEmail(request.getEmail());
            if (exist != null) {
                throw new BusinessException(ResultCode.AUTH_EMAIL_REGISTERED, "该邮箱已注册。");
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

        AuthResponse response = new AuthResponse();
        response.setUserId(user.getId());
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), true);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(jwtTokenProvider.getAccessTokenExpiration() / 1000);
        response.setIsGuest(true);
        storeRefreshToken(user.getId(), refreshToken);
        return response;
    }

    /**
     * 刷新 Token。
     * <p>
     * 校验顺序：
     * <ol>
     *   <li>JWT 签名 + 类型 {@code refresh} 有效；</li>
     *   <li>计算 SHA-256 哈希后查库，确认该 refresh token 已落库且未过期、未删除；</li>
     *   <li>用户存在且未逻辑删除；</li>
     *   <li>删除当前 refresh 记录（一次性使用，避免复用），并颁发新的 access + refresh 对。</li>
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
        // refresh token rotation：当前 refresh 记录一次性使用，避免被盗用复用
        refreshTokenMapper.deleteById(stored.getId());

        User user = userMapper.selectById(userId);
        if (user == null || BizConstant.DELETED.equals(user.getDeleted())) {
            throw new BusinessException(ResultCode.AUTH_REFRESH_TOKEN_INVALID, "刷新令牌无效或已过期。");
        }
        return buildAuthResponse(user);
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
