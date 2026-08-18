package com.resume.user.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OAuth state 存储（防 CSRF）：随机 state 与 provider 绑定，10 分钟过期。
 * <p>
 * 优先使用 Redis（多实例安全）；未配置 Redis 时降级为进程内存（单实例适用）。
 * </p>
 */
@Slf4j
@Component
public class OAuthStateStore {

    private static final long TTL_MINUTES = 10;
    private static final String REDIS_KEY_PREFIX = "oauth-state:";

    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, Entry> states = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private ObjectProvider<RedisTemplate<String, String>> redisTemplateProvider;

    /**
     * 生成并记录 state（顺带惰性清理内存过期条目）。
     */
    public String create(String provider) {
        states.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(LocalDateTime.now()));
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        store(state, provider);
        return state;
    }

    /**
     * 校验并消费 state（一次性）。
     */
    public boolean consume(String state, String provider) {
        if (state == null || state.isBlank()) {
            return false;
        }
        RedisTemplate<String, String> redis = redis();
        if (redis != null) {
            try {
                String key = REDIS_KEY_PREFIX + state;
                String stored = redis.opsForValue().get(key);
                redis.delete(key);
                return provider.equals(stored);
            } catch (Exception e) {
                log.warn("Redis consume oauth state failed: {}", e.getMessage());
            }
        }
        Entry entry = states.remove(state);
        return entry != null && entry.provider.equals(provider)
                && entry.expiresAt.isAfter(LocalDateTime.now());
    }

    private void store(String state, String provider) {
        RedisTemplate<String, String> redis = redis();
        if (redis != null) {
            try {
                redis.opsForValue().set(REDIS_KEY_PREFIX + state, provider, Duration.ofMinutes(TTL_MINUTES));
                return;
            } catch (Exception e) {
                log.warn("Redis store oauth state failed, fallback to memory: {}", e.getMessage());
            }
        }
        states.put(state, new Entry(provider, LocalDateTime.now().plusMinutes(TTL_MINUTES)));
    }

    private RedisTemplate<String, String> redis() {
        return redisTemplateProvider == null ? null : redisTemplateProvider.getIfAvailable();
    }

    private record Entry(String provider, LocalDateTime expiresAt) {
    }
}
