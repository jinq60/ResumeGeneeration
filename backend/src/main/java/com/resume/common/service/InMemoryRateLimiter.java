package com.resume.common.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于 Caffeine 的内存限流器（默认）。
 * <p>
 * 固定窗口：windowMs 内超过 maxRequests 拒绝；窗口过期后重置。
 * Caffeine 自动按写入后过期驱逐，避免手动 cleanup 与内存无限增长，
 * 并通过 maximumSize 防止恶意随机 key 撑爆内存。
 * </p>
 */
@Component
@ConditionalOnProperty(name = "app.rate-limit.driver", havingValue = "memory", matchIfMissing = true)
public class InMemoryRateLimiter implements RateLimiter {

    private final Cache<String, WindowCounter> cache = Caffeine.newBuilder()
            .expireAfterWrite(2, TimeUnit.MINUTES)
            .maximumSize(50_000)
            .build();

    @Override
    public boolean tryAcquire(String key, int maxRequests, long windowMs) {
        long now = System.currentTimeMillis();
        WindowCounter counter = cache.get(key, k -> new WindowCounter(now));
        if (counter == null) {
            return false;
        }
        // 窗口过期则重置
        if (now - counter.windowStart > windowMs) {
            WindowCounter fresh = new WindowCounter(now);
            cache.put(key, fresh);
            counter = fresh;
        }
        return counter.tryAcquire(maxRequests);
    }

    private static class WindowCounter {
        final long windowStart;
        final AtomicInteger count = new AtomicInteger(0);

        WindowCounter(long windowStart) {
            this.windowStart = windowStart;
        }

        boolean tryAcquire(int maxRequests) {
            return count.incrementAndGet() <= maxRequests;
        }
    }
}
