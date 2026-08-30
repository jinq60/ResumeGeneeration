package com.resume.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 游客会话创建限额。
 * <p>
 * 按 IP 每日窗口计数，防止通过 /auth/guest 无限刷号。
 * 优先使用 Redis（多实例安全）；未配置 Redis 时降级为进程内存（单实例适用）。
 * </p>
 */
@Slf4j
@Component
public class GuestAccountGuard {

    private static final String REDIS_KEY_PREFIX = "guest-count:";

    private final Map<String, DailyCounter> counters = new ConcurrentHashMap<>();

    @Value("${app.auth.guest.max-per-ip-per-day:50}")
    private int maxPerIpPerDay;

    @Autowired(required = false)
    private ObjectProvider<RedisTemplate<String, String>> redisTemplateProvider;

    /**
     * 尝试为指定 IP 创建一个游客会话配额。
     *
     * @return true 表示允许创建
     */
    public boolean tryAcquire(String ip) {
        if (ip == null || ip.isBlank()) {
            ip = "unknown";
        }

        RedisTemplate<String, String> redis = redis();
        if (redis != null) {
            try {
                LocalDate today = LocalDate.now();
                String key = REDIS_KEY_PREFIX + ip + ":" + today.format(DateTimeFormatter.BASIC_ISO_DATE);
                Long count = redis.opsForValue().increment(key);
                if (count != null) {
                    Duration ttl = Duration.between(LocalDateTime.now(), today.plusDays(1).atStartOfDay());
                    // 每次重设 TTL，避免首次 expire 失败导致 key 永久有效
                    redis.expire(key, ttl);
                }
                boolean allowed = count != null && count <= maxPerIpPerDay;
                if (!allowed) {
                    log.warn("Guest account limit exceeded: ip={}, maxPerDay={}", ip, maxPerIpPerDay);
                }
                return allowed;
            } catch (Exception e) {
                log.warn("Redis guest count failed, fallback to memory: {}", e.getMessage());
            }
        }

        LocalDate today = LocalDate.now();
        DailyCounter counter = counters.compute(ip, (k, existing) -> {
            if (existing == null || !today.equals(existing.day)) {
                return new DailyCounter(today);
            }
            return existing;
        });
        boolean allowed = counter.count.incrementAndGet() <= maxPerIpPerDay;
        if (!allowed) {
            log.warn("Guest account limit exceeded: ip={}, maxPerDay={}", ip, maxPerIpPerDay);
        }
        return allowed;
    }

    private RedisTemplate<String, String> redis() {
        return redisTemplateProvider == null ? null : redisTemplateProvider.getIfAvailable();
    }

    private static class DailyCounter {
        final LocalDate day;
        final AtomicInteger count = new AtomicInteger(0);

        DailyCounter(LocalDate day) {
            this.day = day;
        }
    }
}
