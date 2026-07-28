package com.resume.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * 基于 Redis 的分布式限流器实现。
 * <p>
 * 使用 Lua 脚本保证原子性，适用于多实例集群部署。
 * 激活条件：classpath 中存在 Redis 客户端 + 显式配置 {@code app.rate-limit.driver=redis}。
 * 未显式启用时回退到 {@link InMemoryRateLimiter}，避免开发环境误启用 Redis 失败。
 * </p>
 */
@Slf4j
@Component
@ConditionalOnClass(RedisTemplate.class)
@ConditionalOnProperty(name = "app.rate-limit.driver", havingValue = "redis")
@RequiredArgsConstructor
public class RedisRateLimiter implements RateLimiter {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String LUA_SCRIPT = """
            local key = KEYS[1]
            local max = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])

            local current = redis.call('INCR', key)
            if current == 1 then
                redis.call('PEXPIRE', key, window)
            end

            if current > max then
                return 0
            end
            return 1
            """;

    private final RedisScript<Long> script = RedisScript.of(LUA_SCRIPT, Long.class);

    @Override
    public boolean tryAcquire(String key, int maxRequests, long windowMs) {
        try {
            Long result = redisTemplate.execute(
                    script,
                    List.of(key),
                    String.valueOf(maxRequests),
                    String.valueOf(windowMs));
            return result != null && result == 1L;
        } catch (Exception e) {
            log.warn("Redis rate limiter failed, allowing request: {}", e.getMessage());
            return true;
        }
    }
}
