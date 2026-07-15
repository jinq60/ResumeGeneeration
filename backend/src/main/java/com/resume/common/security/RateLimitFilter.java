package com.resume.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.common.constant.ResultCode;
import com.resume.common.entity.R;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 简易内存限流过滤器。
 * <p>
 * 基于固定时间窗口的请求计数，达到上限后返回 429。
 * 每个 key 的窗口创建/重置与计数增加通过 {@link ConcurrentHashMap#compute} 原子完成；
 * 窗口创建时使用 {@link System#currentTimeMillis()}，避免 cleanup 误删刚重置的计数器。
 * </p>
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final int maxRequests;
    private final long windowMs;
    private final ObjectMapper objectMapper;

    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanupTime = new AtomicLong(0);

    public RateLimitFilter(
            @Value("${app.rate-limit.max-requests:60}") int maxRequests,
            @Value("${app.rate-limit.window-seconds:60}") long windowSeconds,
            ObjectMapper objectMapper) {
        this.maxRequests = maxRequests;
        this.windowMs = windowSeconds * 1000L;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String key = resolveKey(request);

        // 使用 compute 原子地完成窗口创建/重置与计数增加，避免 cleanup 与窗口更新之间出现竞态。
        boolean[] allowed = new boolean[1];
        counters.compute(key, (k, existing) -> {
            long now = System.currentTimeMillis();
            WindowCounter counter = (existing == null || now - existing.getWindowStart() > windowMs)
                    ? new WindowCounter(now)
                    : existing;
            allowed[0] = counter.tryAcquire(maxRequests);
            return counter;
        });

        if (allowed[0]) {
            chain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded: key={}", key);
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(objectMapper.writeValueAsString(
                    R.error(ResultCode.RATE_LIMITED, "请求过于频繁，请稍后再试。")));
        }

        // 窗口级别的清理不会过于频繁地执行，避免每次请求都全量扫描。
        tryCleanup();
    }

    private String resolveKey(HttpServletRequest request) {
        // 不直接信任 X-Forwarded-For，防止客户端伪造 IP 绕过限流。
        // 如需获取真实客户端 IP，应在可信反向代理后统一部署，由网关统一注入并校验。
        return "rate:" + request.getRemoteAddr();
    }

    /**
     * 按窗口周期触发过期计数器清理，防止内存无限增长。
     */
    private void tryCleanup() {
        long now = System.currentTimeMillis();
        long last = lastCleanupTime.get();
        if (now - last > windowMs && lastCleanupTime.compareAndSet(last, now)) {
            cleanupExpiredCounters(now);
        }
    }

    /**
     * 清理过期计数器。
     * <p>
     * 使用 {@link ConcurrentHashMap#compute(Object, java.util.function.BiFunction)} 按 key
     * 原子地删除或保留，避免与窗口重置发生竞态。
     * </p>
     */
    private void cleanupExpiredCounters(long now) {
        for (String key : new ArrayList<>(counters.keySet())) {
            counters.compute(key, (k, counter) -> {
                if (counter == null) {
                    return null;
                }
                return now - counter.getWindowStart() > windowMs ? null : counter;
            });
        }
    }

    private static class WindowCounter {
        private final long windowStart;
        private final AtomicInteger count = new AtomicInteger(0);

        WindowCounter(long windowStart) {
            this.windowStart = windowStart;
        }

        long getWindowStart() {
            return windowStart;
        }

        boolean tryAcquire(int maxRequests) {
            return count.incrementAndGet() <= maxRequests;
        }
    }
}
