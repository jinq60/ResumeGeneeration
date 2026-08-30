package com.resume.user.auth;

import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.user.config.AuthProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * 邮箱验证码服务：生成、校验（一次性、5 分钟过期、5 次错误作废）与发送。
 * <p>
 * 存储策略：优先使用 Redis（key {@code email-code:{email}}，多实例安全）；
 * 未配置 Redis 时降级为进程内存 Map（单实例适用，重启即失效）。
 * 未配置 SMTP 时降级为日志输出验证码（仅限本地开发演示），
 * 生产环境必须配置 {@code app.auth.smtp.*}。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailCodeService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String REDIS_KEY_PREFIX = "email-code:";
    private static final String DAILY_KEY_PREFIX = "email-daily:";
    private static final int MAX_ATTEMPTS = 5;
    private static final int DAILY_LIMIT_PER_EMAIL = 10;

    private final AuthProperties authProperties;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final ObjectProvider<RedisTemplate<String, String>> redisTemplateProvider;
    private final ObjectProvider<Environment> environmentProvider;

    private final Cache<String, Entry> codes = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();
    private final Cache<String, Integer> dailyCounts = Caffeine.newBuilder()
            .expireAfterWrite(25, TimeUnit.HOURS)
            .maximumSize(10_000)
            .build();

    /**
     * 发送验证码到邮箱（带重发间隔限制）。
     * <p>
     * 先投递成功再落库冷却标记：SMTP 抖动失败时用户可立即重试，不会被无辜冷却。
     * </p>
     */
    public void send(String email) {
        String key = normalizeEmail(email);
        if (isInCooldown(key)) {
            throw new BusinessException(ResultCode.AUTH_EMAIL_CODE_TOO_FREQUENT,
                    "发送过于频繁，请稍后再试。");
        }
        checkDailyLimit(key);

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        sendMail(email, code);
        store(key, code);
    }

    private void checkDailyLimit(String key) {
        String day = java.time.LocalDate.now().toString();
        RedisTemplate<String, String> redis = redis();
        if (redis != null) {
            try {
                String redisKey = DAILY_KEY_PREFIX + key + ":" + day;
                Long count = redis.opsForValue().increment(redisKey);
                if (count != null) {
                    redis.expire(redisKey, Duration.ofDays(1));
                }
                if (count != null && count > DAILY_LIMIT_PER_EMAIL) {
                    throw new BusinessException(ResultCode.RATE_LIMITED, "今日验证码发送次数已达上限，请明日再试。");
                }
                return;
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.warn("Redis check email daily limit failed, fallback to Caffeine: {}", e.getMessage());
            }
        }
        String memKey = key + "|" + day;
        Integer current = dailyCounts.getIfPresent(memKey);
        int count = (current == null ? 1 : current + 1);
        dailyCounts.put(memKey, count);
        if (count > DAILY_LIMIT_PER_EMAIL) {
            throw new BusinessException(ResultCode.RATE_LIMITED, "今日验证码发送次数已达上限，请明日再试。");
        }
    }

    /**
     * 校验验证码（一次性：成功或超过尝试次数后立即删除）。
     */
    public boolean verify(String email, String code) {
        String key = normalizeEmail(email);
        String stored = load(key);
        if (stored == null) {
            return false;
        }
        if (!stored.equals(code)) {
            int attempts = incrementAttempts(key);
            if (attempts >= MAX_ATTEMPTS) {
                remove(key);
            }
            return false;
        }
        remove(key);
        return true;
    }

    private boolean isInCooldown(String key) {
        LocalDateTime lastSent = readLastSentAt(key);
        return lastSent != null
                && lastSent.plusSeconds(authProperties.getEmailCode().getResendIntervalSeconds())
                .isAfter(LocalDateTime.now());
    }

    private void store(String key, String code) {
        RedisTemplate<String, String> redis = redis();
        if (redis != null) {
            try {
                redis.opsForValue().set(REDIS_KEY_PREFIX + key, code,
                        Duration.ofMinutes(authProperties.getEmailCode().getTtlMinutes()));
                redis.opsForValue().set(REDIS_KEY_PREFIX + key + ":sent", LocalDateTime.now().toString(),
                        Duration.ofMinutes(authProperties.getEmailCode().getTtlMinutes()));
                redis.delete(REDIS_KEY_PREFIX + key + ":attempts");
                return;
            } catch (Exception e) {
                log.warn("Redis store email code failed, fallback to memory: {}", e.getMessage());
            }
        }
        codes.put(key, new Entry(code, LocalDateTime.now().plusMinutes(
                authProperties.getEmailCode().getTtlMinutes()), LocalDateTime.now(), 0));
    }

    private String load(String key) {
        RedisTemplate<String, String> redis = redis();
        if (redis != null) {
            try {
                return redis.opsForValue().get(REDIS_KEY_PREFIX + key);
            } catch (Exception e) {
                log.warn("Redis read email code failed, fallback to memory: {}", e.getMessage());
            }
        }
        Entry entry = codes.getIfPresent(key);
        if (entry == null || entry.expiresAt().isBefore(LocalDateTime.now())) {
            if (entry != null) codes.invalidate(key);
            return null;
        }
        return entry.code();
    }

    private LocalDateTime readLastSentAt(String key) {
        RedisTemplate<String, String> redis = redis();
        if (redis != null) {
            try {
                String raw = redis.opsForValue().get(REDIS_KEY_PREFIX + key + ":sent");
                return raw == null ? null : LocalDateTime.parse(raw);
            } catch (Exception e) {
                log.warn("Redis read email code sentAt failed: {}", e.getMessage());
            }
        }
        Entry entry = codes.getIfPresent(key);
        return entry == null ? null : entry.lastSentAt();
    }

    private int incrementAttempts(String key) {
        RedisTemplate<String, String> redis = redis();
        if (redis != null) {
            try {
                String attemptsKey = REDIS_KEY_PREFIX + key + ":attempts";
                Long attempts = redis.opsForValue().increment(attemptsKey);
                // 首次创建时设置过期时间，避免 attempts 计数 key 残留
                if (attempts != null && attempts == 1L) {
                    redis.expire(attemptsKey,
                            Duration.ofMinutes(authProperties.getEmailCode().getTtlMinutes()));
                }
                return attempts == null ? 1 : attempts.intValue();
            } catch (Exception e) {
                log.warn("Redis increment attempts failed: {}", e.getMessage());
            }
        }
        Entry entry = codes.getIfPresent(key);
        if (entry == null) {
            return 0;
        }
        Entry updated = new Entry(entry.code(), entry.expiresAt(), entry.lastSentAt(), entry.attempts() + 1);
        codes.put(key, updated);
        return updated.attempts();
    }

    private void remove(String key) {
        RedisTemplate<String, String> redis = redis();
        if (redis != null) {
            try {
                redis.delete(REDIS_KEY_PREFIX + key);
                redis.delete(REDIS_KEY_PREFIX + key + ":sent");
                redis.delete(REDIS_KEY_PREFIX + key + ":attempts");
                return;
            } catch (Exception e) {
                log.warn("Redis delete email code failed: {}", e.getMessage());
            }
        }
        codes.invalidate(key);
    }

    private RedisTemplate<String, String> redis() {
        return redisTemplateProvider.getIfAvailable();
    }

    private void sendMail(String email, String code) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        AuthProperties.SmtpConfig smtp = authProperties.getSmtp();
        if (mailSender == null || StringUtils.isBlank(smtp.getHost())) {
            // 仅 dev/test 允许将验证码输出到日志供本地联调；
            // 生产环境未配置 SMTP 时必须 fail-fast，防止验证码泄露到日志导致任意账号接管
            if (isDevOrTestProfile()) {
                log.warn("SMTP not configured, email verify code not sent to {}", email);
                log.info("[DEV] Email verify code for {}: {}", email, code);
                return;
            }
            log.error("Email verify code requested but SMTP not configured; refusing to send (prod).");
            throw new BusinessException(ResultCode.AUTH_EMAIL_CODE_SEND_FAILED,
                    "邮箱验证码服务未就绪，请联系管理员或使用密码登录。");
        }
        try {
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

    private boolean isDevOrTestProfile() {
        Environment environment = environmentProvider.getIfAvailable();
        if (environment == null) {
            return false;
        }
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "dev".equals(profile) || "test".equals(profile));
    }

    private record Entry(String code, LocalDateTime expiresAt, LocalDateTime lastSentAt, int attempts) {
    }
}
