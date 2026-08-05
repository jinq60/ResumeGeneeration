package com.resume.user.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.resume.common.constant.BizConstant;
import com.resume.user.entity.User;
import com.resume.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * 首个管理员引导初始化。
 * <p>
 * 生产环境配置 {@code ADMIN_BOOTSTRAP_PHONE} + {@code ADMIN_BOOTSTRAP_PASSWORD} 后，
 * 系统在启动时检测到不存在任何 ADMIN 角色用户时会自动创建首个管理员，
 * 解决"首个管理员账号无法创建"的鸡生蛋问题。
 * 安全约束：
 * <ul>
 *   <li>仅当系统尚无任何 ADMIN 时才生效，已有管理员则跳过（防后门/覆盖）；</li>
 *   <li>仅接受手机号格式账号，密码遵循与注册一致的强度规则；</li>
 *   <li>初始化完成后建议立即移除环境变量或修改密码。</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Pattern PHONE = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern PASSWORD_COMPLEXITY = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).+$");

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.bootstrap.phone:}")
    private String bootstrapPhone;

    @Value("${app.admin.bootstrap.password:}")
    private String bootstrapPassword;

    @Value("${app.admin.bootstrap.nickname:管理员}")
    private String bootstrapNickname;

    @Override
    public void run(ApplicationArguments args) {
        if (StringUtils.isBlank(bootstrapPhone) && StringUtils.isBlank(bootstrapPassword)) {
            log.debug("Admin bootstrap not configured, skip");
            return;
        }
        if (StringUtils.isBlank(bootstrapPhone) || StringUtils.isBlank(bootstrapPassword)) {
            log.warn("Admin bootstrap misconfigured: phone and password must both be set, skip");
            return;
        }
        if (!PHONE.matcher(bootstrapPhone).matches()) {
            log.warn("Admin bootstrap skipped: phone format invalid");
            return;
        }
        if (bootstrapPassword.length() < 8 || bootstrapPassword.length() > 32
                || !PASSWORD_COMPLEXITY.matcher(bootstrapPassword).matches()) {
            log.warn("Admin bootstrap skipped: password must be 8-32 chars with letters and digits");
            return;
        }

        long adminCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getRole, BizConstant.USER_ROLE_ADMIN)
                        .eq(User::getDeleted, BizConstant.NOT_DELETED));
        if (adminCount > 0) {
            log.info("Admin bootstrap skipped: admin account already exists (count={})", adminCount);
            return;
        }

        User admin = new User();
        admin.setPhone(bootstrapPhone);
        admin.setPasswordHash(passwordEncoder.encode(bootstrapPassword));
        admin.setNickname(bootstrapNickname);
        admin.setIsGuest(BizConstant.IS_NOT_GUEST);
        admin.setRole(BizConstant.USER_ROLE_ADMIN);
        admin.setStatus(BizConstant.USER_STATUS_ACTIVE);
        admin.setDeleted(BizConstant.NOT_DELETED);
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(admin);
        log.info("Admin bootstrap success: userId={}, phone={}", admin.getId(),
                maskPhone(bootstrapPhone));
    }

    private String maskPhone(String phone) {
        if (StringUtils.isBlank(phone) || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
