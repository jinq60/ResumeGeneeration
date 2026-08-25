package com.resume.user.auth;

import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.user.config.AuthProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
 * 发送策略：仅当配置了真实短信通道（{@code app.auth.sms.api-key} 非空）才对外开放；
 * 未配置通道时生产环境直接拒绝（不落 Redis），dev/test 环境仍打印验证码便于联调，
 * 生产环境绝不打印明文。另有每手机号每日发送上限防刷。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsCodeService {

    private static final Pattern PHONE = Pattern.compile("^1[3-9]\\d{9}$");
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String REDIS_KEY_PREFIX = "sms-code:";
    private static final String DAILY_KEY_PREFIX = "sms-daily:";
    private static final int MAX_ATTEMPTS = 5;

    private final AuthProperties authProperties;
    private final ObjectProvider<RedisTemplate<String, String>> redisTemplateProvider;

    private final Map<String, Entry> codes = new ConcurrentHashMap<>();
    /** 内存降级模式的每日发送计数：key = phone|yyyy-MM-dd。 */
    private final Map<String, Integer> dailyCounts = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private Environment environment;

    @Value("${app.auth.sms.api-key:}")
    private String smsApiKey;

    @Value("${app.auth.sms.daily-limit-per-phone:10}")
    private int dailyLimitPerPhone;

    /**
     * 短信通道是否已配置真实凭据。
     */
    public boolean isConfigured() {
        return smsApiKey != null && !smsApiKey.isBlank();
    }

    /**
     * 发送验证码到手机号（带重发间隔限制与每日上限）。
     */
    public void send(String phone) {
        if (phone == null || !PHONE.matcher(phone).matches()) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "手机号格式不正确。");
        }
        // 半开放防线：未接入真实短信通道时，非 dev/test 环境直接拒绝，
        // 不再生成/落库验证码（避免占位验证码在生产裸奔）
        if (!isConfigured() && !isDevOrTestProfile()) {
            log.warn("SMS channel not configured, reject sending verify code");
            throw new BusinessException(ResultCode.AUTH_SMS_CODE_NOT_AVAILABLE,
                    "短信服务暂不可用，请使用其他登录方式。");
        }
        checkDailyLimit(phone);
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
            log.info("SMS verify code sent to {}", phone);
        }
    }

    /**
     * 每手机号每日发送上限（默认 10 条，可经 {@code app.auth.sms.daily-limit-per-phone} 配置）。
     * 计数优先走 Redis（INCR 后每次重设 TTL 补偿 expire 失败），未配置 Redis 时降级内存。
     */
    private void checkDailyLimit(String phone) {
        String day = java.time.LocalDate.now().toString();
        RedisTemplate<String, String> redis = redis();
        if (redis != null) {
            try {
                String key = DAILY_KEY_PREFIX + phone + ":" + day;
                Long count = redis.opsForValue().increment(key);
                if (count != null) {
                    redis.expire(key, Duration.ofDays(1));
                }
                if (count != null && count > dailyLimitPerPhone) {
                    throw new BusinessException(ResultCode.RATE_LIMITED,
                            "今日验证码发送次数已达上限，请明日再试。");
                }
                return;
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.warn("Redis check sms daily limit failed, fallback to memory: {}", e.getMessage());
            }
        }
        String memKey = phone + "|" + day;
        int count = dailyCounts.compute(memKey, (k, v) -> v == null ? 1 : v + 1);
        // 清理过期计数，避免长期驻留
        dailyCounts.keySet().removeIf(k -> !k.endsWith("|" + day));
        if (count > dailyLimitPerPhone) {
            throw new BusinessException(ResultCode.RATE_LIMITED,
                    "今日验证码发送次数已达上限，请明日再试。");
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
