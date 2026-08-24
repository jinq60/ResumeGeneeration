package com.resume.user.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.user.dto.AuthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OAuth 登录一次性授权码存储：回调成功后签发短时效 code，
 * 前端用 code 换取令牌对，避免 JWT 经重定向 URL 泄露到浏览器历史/访问日志。
 * <p>
 * code 为 32 字节随机数、单次消费、2 分钟过期；优先 Redis（多实例安全），
 * 未配置 Redis 时降级为进程内存（单实例适用，容量上限防内存放大）。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuthLoginCodeStore {

    private static final long TTL_SECONDS = 120;
    private static final int MAX_MEMORY_ENTRIES = 10_000;
    private static final String REDIS_KEY_PREFIX = "oauth-login-code:";

    private final SecureRandom secureRandom = new SecureRandom();
    private final ObjectMapper objectMapper;
    private final ObjectProvider<RedisTemplate<String, String>> redisTemplateProvider;
    private final Map<String, Entry> codes = new ConcurrentHashMap<>();

    /**
     * 签发一次性授权码（携带登录结果）。
     */
    public String issue(AuthResponse auth) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        store(code, auth);
        return code;
    }

    /**
     * 消费授权码换取登录结果（一次性：无论成败立即删除）。
     */
    public AuthResponse consume(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        RedisTemplate<String, String> redis = redis();
        if (redis != null) {
            try {
                String key = REDIS_KEY_PREFIX + code;
                String stored = redis.opsForValue().get(key);
                redis.delete(key);
                return deserialize(stored);
            } catch (Exception e) {
                log.warn("Redis consume oauth login code failed: {}", e.getMessage());
            }
        }
        Entry entry = codes.remove(code);
        if (entry == null || entry.expiresAt().isBefore(LocalDateTime.now())) {
            return null;
        }
        return deserialize(entry.payload());
    }

    private void store(String code, AuthResponse auth) {
        String payload = serialize(auth);
        RedisTemplate<String, String> redis = redis();
        if (redis != null) {
            try {
                redis.opsForValue().set(REDIS_KEY_PREFIX + code, payload,
                        Duration.ofSeconds(TTL_SECONDS));
                return;
            } catch (Exception e) {
                log.warn("Redis store oauth login code failed, fallback to memory: {}", e.getMessage());
            }
        }
        // 容量上限 + 惰性清理过期条目，防止匿名刷码导致内存放大
        codes.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(LocalDateTime.now()));
        if (codes.size() >= MAX_MEMORY_ENTRIES) {
            log.warn("OAuth login code memory store full, rejecting new code issuance");
            codes.clear();
        }
        codes.put(code, new Entry(payload, LocalDateTime.now().plusSeconds(TTL_SECONDS)));
    }

    private String serialize(AuthResponse auth) {
        try {
            return objectMapper.writeValueAsString(auth);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize oauth login response", e);
        }
    }

    private AuthResponse deserialize(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(payload, AuthResponse.class);
        } catch (Exception e) {
            log.warn("Failed to deserialize oauth login response: {}", e.getMessage());
            return null;
        }
    }

    private RedisTemplate<String, String> redis() {
        return redisTemplateProvider.getIfAvailable();
    }

    private record Entry(String payload, LocalDateTime expiresAt) {
    }
}
