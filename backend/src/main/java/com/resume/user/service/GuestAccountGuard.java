package com.resume.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 游客会话创建限额（内存实现，单实例部署适用）。
 * <p>
 * 按 IP 每日窗口计数，防止通过 /auth/guest 无限刷号；
 * 计数器惰性过期（跨日自动重置），无需定时清理。
 * </p>
 */
@Slf4j
@Component
public class GuestAccountGuard {

    private final Map<String, DailyCounter> counters = new ConcurrentHashMap<>();

    @Value("${app.auth.guest.max-per-ip-per-day:50}")
    private int maxPerIpPerDay;

    /**
     * 尝试为指定 IP 创建一个游客会话配额。
     *
     * @return true 表示允许创建
     */
    public boolean tryAcquire(String ip) {
        if (ip == null || ip.isBlank()) {
            ip = "unknown";
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

    private static class DailyCounter {
        final LocalDate day;
        final AtomicInteger count = new AtomicInteger(0);

        DailyCounter(LocalDate day) {
            this.day = day;
        }
    }
}
