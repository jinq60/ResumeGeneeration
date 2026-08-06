package com.resume.user.auth;

import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.user.config.AuthProperties;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 邮箱验证码服务：生成、校验（一次性、5 分钟过期）与发送。
 * <p>
 * 未配置 SMTP 时降级为日志输出验证码（仅限本地开发演示），
 * 生产环境必须配置 {@code app.auth.smtp.*}。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailCodeService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AuthProperties authProperties;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    private final Map<String, Entry> codes = new ConcurrentHashMap<>();

    /**
     * 发送验证码到邮箱（带重发间隔限制）。
     */
    public void send(String email) {
        String key = normalizeEmail(email);
        Entry existing = codes.get(key);
        if (existing != null && existing.lastSentAt != null
                && existing.lastSentAt.plusSeconds(authProperties.getEmailCode().getResendIntervalSeconds())
                .isAfter(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.AUTH_EMAIL_CODE_TOO_FREQUENT,
                    "发送过于频繁，请稍后再试。");
        }

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        codes.put(key, new Entry(code, LocalDateTime.now().plusMinutes(
                authProperties.getEmailCode().getTtlMinutes()), LocalDateTime.now()));
        sendMail(email, code);
    }

    /**
     * 校验验证码（一次性：成功后立即删除）。
     */
    public boolean verify(String email, String code) {
        String key = normalizeEmail(email);
        Entry entry = codes.get(key);
        if (entry == null || !entry.code.equals(code)) {
            return false;
        }
        codes.remove(key);
        return entry.expiresAt.isAfter(LocalDateTime.now());
    }

    private void sendMail(String email, String code) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        AuthProperties.SmtpConfig smtp = authProperties.getSmtp();
        if (mailSender == null || StringUtils.isBlank(smtp.getHost())) {
            log.warn("[DEV] Email verify code for {}: {}", email, code);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(StringUtils.defaultString(smtp.getFrom(), smtp.getUsername()));
            mail.setTo(email);
            mail.setSubject("【智能简历】登录验证码");
            mail.setText("您的登录验证码是：" + code + "，"
                    + authProperties.getEmailCode().getTtlMinutes() + " 分钟内有效。若非本人操作请忽略。");
            mailSender.send(mail);
            log.info("Email verify code sent to {}", email);
        } catch (Exception e) {
            log.error("Send email verify code failed: {}", e.getMessage());
            throw new BusinessException(ResultCode.AUTH_EMAIL_CODE_SEND_FAILED,
                    "验证码发送失败，请稍后重试。");
        }
    }

    private String normalizeEmail(String email) {
        return StringUtils.trim(email).toLowerCase();
    }

    private record Entry(String code, LocalDateTime expiresAt, LocalDateTime lastSentAt) {
    }
}
