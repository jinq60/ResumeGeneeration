package com.resume.user.auth;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OAuth state 存储（防 CSRF）：随机 state 与 provider 绑定，10 分钟过期。
 * 单实例内存实现，多实例部署时建议替换为 Redis。
 */
@Component
public class OAuthStateStore {

    private static final long TTL_MINUTES = 10;

    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, Entry> states = new ConcurrentHashMap<>();

    /**
     * 生成并记录 state（顺带惰性清理过期条目）。
     */
    public String create(String provider) {
        states.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(LocalDateTime.now()));
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        states.put(state, new Entry(provider, LocalDateTime.now().plusMinutes(TTL_MINUTES)));
        return state;
    }

    /**
     * 校验并消费 state（一次性）。
     */
    public boolean consume(String state, String provider) {
        if (state == null || state.isBlank()) {
            return false;
        }
        Entry entry = states.remove(state);
        return entry != null && entry.provider.equals(provider)
                && entry.expiresAt.isAfter(LocalDateTime.now());
    }

    private record Entry(String provider, LocalDateTime expiresAt) {
    }
}
