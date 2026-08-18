package com.resume.user.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.resume.common.constant.BizConstant;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.common.service.AuditLogService;
import com.resume.user.entity.User;
import com.resume.user.entity.UserAuth;
import com.resume.user.mapper.UserAuthMapper;
import com.resume.user.mapper.UserMapper;
import com.resume.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * 多方式认证编排：邮箱验证码登录、第三方 OAuth 登录与账号绑定。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private final UserMapper userMapper;
    private final UserAuthMapper userAuthMapper;
    private final UserService userService;
    private final EmailCodeService emailCodeService;
    private final SmsCodeService smsCodeService;
    private final AuditLogService auditLogService;

    /**
     * 邮箱验证码登录：校验验证码后按邮箱查找用户，不存在则自动创建。
     */
    @Transactional(rollbackFor = Exception.class)
    public com.resume.user.dto.AuthResponse authenticateByEmailCode(String email, String code) {
        if (StringUtils.isBlank(email) || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "邮箱格式不正确。");
        }
        if (!emailCodeService.verify(email, code)) {
            throw new BusinessException(ResultCode.AUTH_EMAIL_CODE_INVALID, "验证码不正确或已过期。");
        }
        String normalized = email.trim().toLowerCase();
        User user = findByEmail(normalized);
        if (user == null) {
            user = createUser(normalized, normalized.substring(0, normalized.indexOf('@')), null);
            log.info("Email-code login auto-created user: userId={}", user.getId());
        }
        if (BizConstant.USER_STATUS_DISABLED.equals(user.getStatus())) {
            throw new BusinessException(ResultCode.AUTH_ACCOUNT_LOCKED, "账号已被锁定，请稍后再试。");
        }
        auditLogService.record(user.getId(), "login", user.getId(), "loginType=email_code");
        return userService.buildAuthResponse(user);
    }

    /**
     * 短信验证码登录：校验验证码后按手机号查找用户，不存在则自动创建。
     */
    @Transactional(rollbackFor = Exception.class)
    public com.resume.user.dto.AuthResponse authenticateBySmsCode(String phone, String code) {
        if (StringUtils.isBlank(phone) || !PHONE_PATTERN.matcher(phone.trim()).matches()) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "手机号格式不正确。");
        }
        if (!smsCodeService.verify(phone, code)) {
            throw new BusinessException(ResultCode.AUTH_SMS_CODE_INVALID, "验证码不正确或已过期。");
        }
        String normalized = phone.trim();
        User user = findByPhone(normalized);
        if (user == null) {
            user = createUserByPhone(normalized);
            log.info("SMS-code login auto-created user: userId={}", user.getId());
        }
        if (BizConstant.USER_STATUS_DISABLED.equals(user.getStatus())) {
            throw new BusinessException(ResultCode.AUTH_ACCOUNT_LOCKED, "账号已被锁定，请稍后再试。");
        }
        auditLogService.record(user.getId(), "login", user.getId(), "loginType=sms_code");
        return userService.buildAuthResponse(user);
    }

    /**
     * OAuth 登录：按 provider+accountId 查找绑定，未绑定则创建用户并绑定。
     */
    @Transactional(rollbackFor = Exception.class)
    public com.resume.user.dto.AuthResponse authenticateByOAuth(OAuthUserInfo info) {
        String accountId = StringUtils.defaultString(info.providerAccountId()).trim();
        if (StringUtils.isBlank(accountId)) {
            throw new BusinessException(ResultCode.AUTH_OAUTH_EXCHANGE_FAILED, "第三方登录失败：缺少账号标识。");
        }
        UserAuth binding = findBinding(info.provider(), accountId);
        User user;
        if (binding != null) {
            user = userMapper.selectById(binding.getUserId());
        } else {
            user = null;
        }
        if (user == null || BizConstant.DELETED.equals(user.getDeleted())) {
            // 仅当第三方邮箱已验证时才按邮箱关联既有账号，防止攻击者用未验证邮箱接管他人账号
            user = info.emailVerified() && StringUtils.isNotBlank(info.email())
                    ? findByEmail(info.email().trim().toLowerCase())
                    : null;
            if (user == null) {
                user = createUser(info.email(),
                        StringUtils.defaultString(info.nickname(), "用户"), info.avatarUrl());
            }
            bindUser(user.getId(), info.provider(), accountId);
            log.info("OAuth bound: provider={}, userId={}", info.provider(), user.getId());
        }
        if (BizConstant.USER_STATUS_DISABLED.equals(user.getStatus())) {
            throw new BusinessException(ResultCode.AUTH_ACCOUNT_LOCKED, "账号已被锁定，请稍后再试。");
        }
        auditLogService.record(user.getId(), "login", user.getId(),
                "loginType=" + info.provider());
        return userService.buildAuthResponse(user);
    }

    private User createUser(String email, String nickname, String avatarUrl) {
        User user = new User();
        user.setEmail(StringUtils.isBlank(email) ? null : email.trim().toLowerCase());
        user.setNickname(StringUtils.defaultIfBlank(nickname, "用户"));
        user.setAvatarUrl(avatarUrl);
        user.setIsGuest(BizConstant.IS_NOT_GUEST);
        user.setRole(BizConstant.USER_ROLE_USER);
        user.setStatus(BizConstant.USER_STATUS_ACTIVE);
        user.setDeleted(BizConstant.NOT_DELETED);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }

    private User findByEmail(String email) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, email)
                .eq(User::getDeleted, BizConstant.NOT_DELETED)
                .last("LIMIT 1");
        return userMapper.selectOne(wrapper);
    }

    private User findByPhone(String phone) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, phone)
                .eq(User::getDeleted, BizConstant.NOT_DELETED)
                .last("LIMIT 1");
        return userMapper.selectOne(wrapper);
    }

    private User createUserByPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickname("用户" + phone.substring(phone.length() - 4));
        user.setIsGuest(BizConstant.IS_NOT_GUEST);
        user.setRole(BizConstant.USER_ROLE_USER);
        user.setStatus(BizConstant.USER_STATUS_ACTIVE);
        user.setDeleted(BizConstant.NOT_DELETED);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }

    private UserAuth findBinding(String provider, String account) {
        LambdaQueryWrapper<UserAuth> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAuth::getProvider, provider)
                .eq(UserAuth::getAccount, account)
                .eq(UserAuth::getDeleted, BizConstant.NOT_DELETED)
                .last("LIMIT 1");
        return userAuthMapper.selectOne(wrapper);
    }

    private void bindUser(String userId, String provider, String account) {
        UserAuth binding = new UserAuth();
        binding.setUserId(userId);
        binding.setProvider(provider);
        binding.setAccount(account);
        binding.setDeleted(BizConstant.NOT_DELETED);
        binding.setCreatedAt(LocalDateTime.now());
        binding.setUpdatedAt(LocalDateTime.now());
        userAuthMapper.insert(binding);
    }
}
