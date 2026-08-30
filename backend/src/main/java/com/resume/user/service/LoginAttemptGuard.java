package com.resume.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录失败锁定。
 * <p>
 * 双维度防护：
 * <ul>
 *   <li>账号维度：连续失败达到上限后锁定一段时间，防止定向密码爆破（锁定策略不变）；</li>
 *   <li>IP 维度：单 IP 窗口内失败达到更高上限后拒绝，防止密码喷洒与跨账号撞库，
 *       同时缓解"仅按账号锁定"导致的定向锁号 DoS（攻击者锁号不影响其他 IP 正常登录）。</li>
 * </ul>
 * 成功登录或锁定到期后自动复位。
 * 优先使用 Redis（多实例安全），INCR 后每次都重设 TTL（补偿 expire 写失败场景）；
 * 未配置 Redis 时降级为进程内存（单实例适用）。
 * </p>
 */
@Slf4j
@Component
public class LoginAttemptGuard {

    private static final String FAIL_KEY_PREFIX = "login-fail:";
    private static final String LOCK_KEY_PREFIX = "login-lock:";
    private static final String IP_FAIL_KEY_PREFIX = "login-fail:ip:";

    /** 进程内存中 IP 维度状态的 key 前缀（与 Redis key 语义对齐，避免与账号冲突）。 */
    private static final String IP_MEMORY_KEY_PREFIX = "ip:";

    private final Map<String, AttemptState> states = new ConcurrentHashMap<>();

    @Value("${app.auth.login.max-failures:5}")
    private int maxFailures;

    @Value("${app.auth.login.lockout-minutes:15}")
    private long lockoutMinutes;

    /** 单 IP 窗口内允许的登录失败上限（密码喷洒/撞库防御，阈值高于账号维度）。 */
    @Value("${app.auth.login.ip-max-failures:20}")
    private int ipMaxFailures;

    @Autowired(required = false)
    private ObjectProvider<RedisTemplate<String, String>> redisTemplateProvider;

    private String normalizeAccount(String account) {
        return account == null ? null : account.trim().toLowerCase();
    }

    /**
     * 账号是否处于锁定状态。
     */
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
                log.warn("Redis check login lock failed, fallback to memory: {}", e.getMessage());
            }
        }
        AttemptState state = states.get(key);
        if (state == null) {
            return false;
        }
        if (state.lockUntil != null && System.currentTimeMillis() >= state.lockUntil) {
            states.remove(key);
            return false;
        }
        return state.failures >= maxFailures;
    }

    /**
     * 该 IP 是否因失败过多被拒绝（窗口随 TTL 自动过期）。
     */
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
                log.warn("Redis check ip login failures failed, fallback to memory: {}", e.getMessage());
            }
        }
        AttemptState state = states.get(IP_MEMORY_KEY_PREFIX + ip);
        if (state == null) {
            return false;
        }
        if (state.lockUntil != null && System.currentTimeMillis() >= state.lockUntil) {
            states.remove(IP_MEMORY_KEY_PREFIX + ip);
            return false;
        }
        return state.failures >= ipMaxFailures;
    }

    /**
     * 记录一次登录失败；达到上限时触发锁定。
     */
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
                // 每次都重设 TTL：即使首次 expire 因异常丢失也不会导致计数永不过期
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
                log.warn("Redis record login failure failed, fallback to memory: {}", e.getMessage());
            }
        }
        if (states.size() > 20000 && !states.containsKey(key)) {
            // 防御：随机账号撑大内存
            states.entrySet().removeIf(e -> e.getValue().lockUntil != null && System.currentTimeMillis() >= e.getValue().lockUntil);
            if (states.size() > 20000) {
                log.warn("LoginAttemptGuard states too large ({}), rejecting new key to prevent OOM", states.size());
                return;
            }
        }
        states.compute(key, (k, state) -> {
            AttemptState next = state == null ? new AttemptState() : state;
            next.failures++;
            if (next.failures >= maxFailures && next.lockUntil == null) {
                next.lockUntil = System.currentTimeMillis() + lockoutMinutes * 60_000L;
                log.warn("Login account locked: account={}, failures={}, lockoutMinutes={}",
                        k, next.failures, lockoutMinutes);
            }
            return next;
        });
    }

    /**
     * 记录一次 IP 维度登录失败；达到上限后该 IP 在窗口内被拒绝。
     * 不在登录成功时复位（防止攻击者通过成功请求重置计数），随 TTL 自然过期。
     */
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
                // 每次都重设 TTL，补偿首次 expire 失败场景
                if (failures != null) {
                    redis.expire(failKey, window);
                }
                if (failures != null && failures >= ipMaxFailures) {
                    log.warn("Login ip blocked: ip={}, failures={}, windowMinutes={}",
                            ip, failures, lockoutMinutes);
                }
                return;
            } catch (Exception e) {
                log.warn("Redis record ip login failure failed, fallback to memory: {}", e.getMessage());
            }
        }
        String memKey = IP_MEMORY_KEY_PREFIX + ip;
        if (states.size() > 20000 && !states.containsKey(memKey)) {
            states.entrySet().removeIf(e -> e.getValue().lockUntil != null && System.currentTimeMillis() >= e.getValue().lockUntil);
            if (states.size() > 20000) {
                log.warn("LoginAttemptGuard IP states too large, rejecting {}", ip);
                return;
            }
        }
        states.compute(memKey, (k, state) -> {
            AttemptState next = state == null ? new AttemptState() : state;
            next.failures++;
            if (next.failures >= ipMaxFailures && next.lockUntil == null) {
                next.lockUntil = System.currentTimeMillis() + lockoutMinutes * 60_000L;
                log.warn("Login ip blocked: ip={}, failures={}, windowMinutes={}",
                        ip, next.failures, lockoutMinutes);
            }
            return next;
        });
    }

    /**
     * 登录成功后复位（仅账号维度）。
     */
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
                    log.warn("Redis reset login state failed, fallback to memory: {}", e.getMessage());
                }
            }
            states.remove(key);
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
