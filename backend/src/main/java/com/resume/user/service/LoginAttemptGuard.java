package com.resume.user.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 登录失败锁定（Caffeine 本地降级）。
 * <p>
 * 双维度防护：
 * <ul>
 *   <li>账号维度：连续失败达到上限后锁定一段时间，防止定向密码爆破；</li>
 *   <li>IP 维度：单 IP 窗口内失败达到更高上限后拒绝，防止密码喷洒；</li>
 * </ul>
 * 优先 Redis，降级 Caffeine（自动过期，无需手动清理）。
 * </p>
 */
@Slf4j
@Component
public class LoginAttemptGuard {

    private static final String FAIL_KEY_PREFIX = "login-fail:";
    private static final String LOCK_KEY_PREFIX = "login-lock:";
    private static final String IP_FAIL_KEY_PREFIX = "login-fail:ip:";
    private static final String IP_MEMORY_KEY_PREFIX = "ip:";

    private final Cache<String, AttemptState> cache = Caffeine.newBuilder()
            .expireAfterWrite(20, TimeUnit.MINUTES)
            .maximumSize(20_000)
            .build();

    @Value("${app.auth.login.max-failures:5}")
    private int maxFailures;

    @Value("${app.auth.login.lockout-minutes:15}")
    private long lockoutMinutes;

    @Value("${app.auth.login.ip-max-failures:20}")
    private int ipMaxFailures;

    @Autowired(required = false)
    private ObjectProvider<RedisTemplate<String, String>> redisTemplateProvider;

    private String normalizeAccount(String account) {
        return account == null ? null : account.trim().toLowerCase();
    }

    public boolean isLocked(String account) {
        String key = normalizeAccount(account);
        if (key == null || key.isEmpty()) {
            return false;
        }
        RedisTemplate<String, String> redis = redis();
        if (redis != null) {
            try {
                return Boolean.TRUE.equals(redis.hasKey(LOCK_KEY_PREFIX + key));
            } catch (Exception e) {
                log.warn("Redis check login lock failed, fallback to Caffeine: {}", e.getMessage());
            }
        }
        AttemptState state = cache.getIfPresent(key);
        if (state == null) {
            return false;
        }
        if (state.lockUntil != null && System.currentTimeMillis() >= state.lockUntil) {
            cache.invalidate(key);
            return false;
        }
        return state.failures >= maxFailures;
    }

    public boolean isIpBlocked(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        RedisTemplate<String, String> redis = redis();
        if (redis != null) {
            try {
                String count = redis.opsForValue().get(IP_FAIL_KEY_PREFIX + ip);
                return count != null && Long.parseLong(count) >= ipMaxFailures;
            } catch (Exception e) {
                log.warn("Redis check ip failures failed, fallback to Caffeine: {}", e.getMessage());
            }
        }
        AttemptState state = cache.getIfPresent(IP_MEMORY_KEY_PREFIX + ip);
        if (state == null) {
            return false;
        }
        if (state.lockUntil != null && System.currentTimeMillis() >= state.lockUntil) {
            cache.invalidate(IP_MEMORY_KEY_PREFIX + ip);
            return false;
        }
        return state.failures >= ipMaxFailures;
    }

    public void recordFailure(String account) {
        String key = normalizeAccount(account);
        if (key == null || key.isEmpty()) {
            return;
        }
        RedisTemplate<String, String> redis = redis();
        if (redis != null) {
            try {
                Duration lockout = Duration.ofMinutes(lockoutMinutes);
                String failKey = FAIL_KEY_PREFIX + key;
                Long failures = redis.opsForValue().increment(failKey);
                if (failures != null) {
                    redis.expire(failKey, lockout);
                }
                if (failures != null && failures >= maxFailures) {
                    redis.opsForValue().set(LOCK_KEY_PREFIX + key, "1", lockout);
                    log.warn("Login account locked: account={}, failures={}, lockoutMinutes={}",
                            account, failures, lockoutMinutes);
                }
                return;
            } catch (Exception e) {
                log.warn("Redis record login failure failed, fallback to Caffeine: {}", e.getMessage());
            }
        }
        AttemptState state = cache.get(key, k -> new AttemptState());
        state.failures++;
        if (state.failures >= maxFailures && state.lockUntil == null) {
            state.lockUntil = System.currentTimeMillis() + lockoutMinutes * 60_000L;
            log.warn("Login account locked: account={}, failures={}, lockoutMinutes={}",
                    key, state.failures, lockoutMinutes);
        }
    }

    public void recordIpFailure(String ip) {
        if (ip == null || ip.isBlank()) {
            return;
        }
        RedisTemplate<String, String> redis = redis();
        if (redis != null) {
            try {
                Duration window = Duration.ofMinutes(lockoutMinutes);
                String failKey = IP_FAIL_KEY_PREFIX + ip;
                Long failures = redis.opsForValue().increment(failKey);
                if (failures != null) {
                    redis.expire(failKey, window);
                }
                if (failures != null && failures >= ipMaxFailures) {
                    log.warn("Login ip blocked: ip={}, failures={}, windowMinutes={}",
                            ip, failures, lockoutMinutes);
                }
                return;
            } catch (Exception e) {
                log.warn("Redis record ip failure failed, fallback to Caffeine: {}", e.getMessage());
            }
        }
        String memKey = IP_MEMORY_KEY_PREFIX + ip;
        AttemptState state = cache.get(memKey, k -> new AttemptState());
        state.failures++;
        if (state.failures >= ipMaxFailures && state.lockUntil == null) {
            state.lockUntil = System.currentTimeMillis() + lockoutMinutes * 60_000L;
            log.warn("Login ip blocked: ip={}, failures={}, windowMinutes={}",
                    ip, state.failures, lockoutMinutes);
        }
    }

    public void reset(String account) {
        String key = normalizeAccount(account);
        if (key != null && !key.isEmpty()) {
            RedisTemplate<String, String> redis = redis();
            if (redis != null) {
                try {
                    redis.delete(FAIL_KEY_PREFIX + key);
                    redis.delete(LOCK_KEY_PREFIX + key);
                    return;
                } catch (Exception e) {
                    log.warn("Redis reset login state failed, fallback to Caffeine: {}", e.getMessage());
                }
            }
            cache.invalidate(key);
        }
    }

    private RedisTemplate<String, String> redis() {
        return redisTemplateProvider == null ? null : redisTemplateProvider.getIfAvailable();
    }

    private static class AttemptState {
        int failures = 0;
        Long lockUntil = null;
    }
}
