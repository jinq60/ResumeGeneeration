package com.resume.user.auth;

import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.user.config.AuthProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 手机短信验证码服务：生成、校验（一次性、5 分钟过期、5 次错误作废）与发送。
 * <p>
 * 存储策略：优先使用 Redis（多实例安全）；未配置 Redis 时降级为进程内存。
 * 发送策略：未接入真实短信通道，仅 dev/test 环境打印验证码便于联调，生产环境绝不打印明文。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsCodeService {

    private static final Pattern PHONE = Pattern.compile("^1[3-9]\\d{9}$");
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String REDIS_KEY_PREFIX = "sms-code:";
    private static final int MAX_ATTEMPTS = 5;

    private final AuthProperties authProperties;
    private final ObjectProvider<RedisTemplate<String, String>> redisTemplateProvider;

    private final Map<String, Entry> codes = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private Environment environment;

    /**
     * 发送验证码到手机号（带重发间隔限制）。
     */
    public void send(String phone) {
        if (phone == null || !PHONE.matcher(phone).matches()) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "手机号格式不正确。");
        }
        if (isInCooldown(phone)) {
            throw new BusinessException(ResultCode.AUTH_SMS_CODE_TOO_FREQUENT,
                    "发送过于频繁，请稍后再试。");
        }

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        store(phone, code);
        sendSms(phone, code);
    }

    /**
     * 校验验证码（一次性：成功或超过尝试次数后立即删除）。
     */
    public boolean verify(String phone, String code) {
        String stored = load(phone);
        if (stored == null) {
            return false;
        }
        if (!stored.equals(code)) {
            int attempts = incrementAttempts(phone);
            if (attempts >= MAX_ATTEMPTS) {
                remove(phone);
            }
            return false;
        }
        remove(phone);
        return true;
    }

    private void sendSms(String phone, String code) {
        // TODO: 接入短信服务商（阿里云/腾讯云）后在此发送真实短信
        if (isDevOrTestProfile()) {
            log.warn("[DEV] SMS verify code for {}: {}", phone, code);
        } else {
            log.warn("SMS channel not configured, verify code not sent to {}", phone);
        }
    }

    private boolean isDevOrTestProfile() {
        if (environment == null) {
            return false;
        }
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> "dev".equals(p) || "test".equals(p));
    }

    private boolean isInCooldown(String phone) {
        LocalDateTime lastSent = readLastSentAt(phone);
        return lastSent != null
                && lastSent.plusSeconds(authProperties.getEmailCode().getResendIntervalSeconds())
                .isAfter(LocalDateTime.now());
    }

    private void store(String phone, String code) {
        RedisTemplate<String, String> redis = redis();
        if (redis != null) {
            try {
                redis.opsForValue().set(REDIS_KEY_PREFIX + phone, code,
                        Duration.ofMinutes(authProperties.getEmailCode().getTtlMinutes()));
                redis.opsForValue().set(REDIS_KEY_PREFIX + phone + ":sent", LocalDateTime.now().toString(),
                        Duration.ofMinutes(authProperties.getEmailCode().getTtlMinutes()));
                redis.delete(REDIS_KEY_PREFIX + phone + ":attempts");
                return;
            } catch (Exception e) {
                log.warn("Redis store sms code failed, fallback to memory: {}", e.getMessage());
            }
        }
        codes.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(LocalDateTime.now()));
        codes.put(phone, new Entry(code, LocalDateTime.now().plusMinutes(
                authProperties.getEmailCode().getTtlMinutes()), LocalDateTime.now(), 0));
    }

    private String load(String phone) {
        RedisTemplate<String, String> redis = redis();
        if (redis != null) {
            try {
                return redis.opsForValue().get(REDIS_KEY_PREFIX + phone);
            } catch (Exception e) {
                log.warn("Redis read sms code failed, fallback to memory: {}", e.getMessage());
            }
        }
        Entry entry = codes.get(phone);
        if (entry == null || entry.expiresAt().isBefore(LocalDateTime.now())) {
            codes.remove(phone);
            return null;
        }
        return entry.code();
    }

    private LocalDateTime readLastSentAt(String phone) {
        RedisTemplate<String, String> redis = redis();
        if (redis != null) {
            try {
                String raw = redis.opsForValue().get(REDIS_KEY_PREFIX + phone + ":sent");
                return raw == null ? null : LocalDateTime.parse(raw);
            } catch (Exception e) {
                log.warn("Redis read sms code sentAt failed: {}", e.getMessage());
            }
        }
        Entry entry = codes.get(phone);
        return entry == null ? null : entry.lastSentAt();
    }

    private int incrementAttempts(String phone) {
        RedisTemplate<String, String> redis = redis();
        if (redis != null) {
            try {
                String attemptsKey = REDIS_KEY_PREFIX + phone + ":attempts";
                Long attempts = redis.opsForValue().increment(attemptsKey);
                if (attempts != null && attempts == 1L) {
                    redis.expire(attemptsKey,
                            Duration.ofMinutes(authProperties.getEmailCode().getTtlMinutes()));
                }
                return attempts == null ? 1 : attempts.intValue();
            } catch (Exception e) {
                log.warn("Redis increment sms attempts failed: {}", e.getMessage());
            }
        }
        Entry updated = codes.compute(phone, (k, entry) -> {
            if (entry == null) {
                return null;
            }
            return new Entry(entry.code(), entry.expiresAt(), entry.lastSentAt(), entry.attempts() + 1);
        });
        return updated == null ? 0 : updated.attempts();
    }

    private void remove(String phone) {
        RedisTemplate<String, String> redis = redis();
        if (redis != null) {
            try {
                redis.delete(REDIS_KEY_PREFIX + phone);
                redis.delete(REDIS_KEY_PREFIX + phone + ":sent");
                redis.delete(REDIS_KEY_PREFIX + phone + ":attempts");
                return;
            } catch (Exception e) {
                log.warn("Redis delete sms code failed: {}", e.getMessage());
            }
        }
        codes.remove(phone);
    }

    private RedisTemplate<String, String> redis() {
        return redisTemplateProvider.getIfAvailable();
    }

    private record Entry(String code, LocalDateTime expiresAt, LocalDateTime lastSentAt, int attempts) {
    }
}
