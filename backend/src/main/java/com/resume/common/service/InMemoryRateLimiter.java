package com.resume.common.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于内存的限流器实现（默认）。
 * <p>
 * 使用 ConcurrentHashMap 存储滑动窗口计数器，定期清理过期条目，适用于单实例部署。
 * 激活条件：未显式启用 {@code app.rate-limit.driver=redis}，即不配置或配置为 {@code memory}。
 * </p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.rate-limit.driver", havingValue = "memory", matchIfMissing = true)
public class InMemoryRateLimiter implements RateLimiter {

    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    private volatile long lastCleanup = System.currentTimeMillis();
    private static final long CLEANUP_INTERVAL_MS = 60_000;

    @Override
    public boolean tryAcquire(String key, int maxRequests, long windowMs) {
        long now = System.currentTimeMillis();
        // 防御：恶意随机 key 撑大内存，超限时拒绝新 key
        if (counters.size() > 50000 && !counters.containsKey(key)) {
            log.warn("InMemoryRateLimiter map too large ({}), rejecting new key to prevent OOM", counters.size());
            return false;
        }

        WindowCounter counter = counters.compute(key, (k, existing) -> {
            if (existing == null || now - existing.windowStart > windowMs) {
                return new WindowCounter(now);
            }
            return existing;
        });

        if (now - lastCleanup > CLEANUP_INTERVAL_MS) {
            synchronized (this) {
                if (now - lastCleanup > CLEANUP_INTERVAL_MS) {
                    cleanupExpired(now, windowMs);
                    lastCleanup = now;
                }
            }
        }

        return counter.tryAcquire(maxRequests);
    }

    private void cleanupExpired(long now, long windowMs) {
        for (String key : new ArrayList<>(counters.keySet())) {
            counters.compute(key, (k, counter) -> {
                if (counter == null) return null;
                return now - counter.windowStart > windowMs * 2 ? null : counter;
            });
        }
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