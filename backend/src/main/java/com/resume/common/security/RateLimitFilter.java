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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 简易内存限流过滤器。
 * <p>
 * 基于固定时间窗口的请求计数，达到上限后返回 429。
 * 默认使用 TCP 连接对端地址 {@link HttpServletRequest#getRemoteAddr()}，不直接信任客户端传入的
 * {@code X-Forwarded-For}，避免通过伪造请求头绕过限流。
 * 过期计数器会在每次请求时清理，防止内存持续增长。
 * </p>
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final int maxRequests;
    private final long windowMs;
    private final ObjectMapper objectMapper;

    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

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

        // 清理过期计数器，防止内存无限增长
        cleanupExpiredCounters();

        WindowCounter counter = counters.computeIfAbsent(key, k -> new WindowCounter());

        if (counter.tryAcquire(maxRequests, windowMs)) {
            chain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded: key={}", key);
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(objectMapper.writeValueAsString(
                    R.error(ResultCode.RATE_LIMITED, "请求过于频繁，请稍后再试。")));
        }
    }

    private String resolveKey(HttpServletRequest request) {
        // 不直接信任 X-Forwarded-For，防止客户端伪造 IP 绕过限流。
        // 如需获取真实客户端 IP，应在可信反向代理后统一部署，由网关统一注入并校验。
        return "rate:" + request.getRemoteAddr();
    }

    private void cleanupExpiredCounters() {
        long now = System.currentTimeMillis();
        counters.entrySet().removeIf(entry -> now - entry.getValue().windowStart > windowMs);
    }

    private static class WindowCounter {
        private volatile long windowStart = System.currentTimeMillis();
        private final AtomicInteger count = new AtomicInteger(0);

        boolean tryAcquire(int maxRequests, long windowMs) {
            long now = System.currentTimeMillis();
            if (now - windowStart > windowMs) {
                synchronized (this) {
                    if (now - windowStart > windowMs) {
                        windowStart = now;
                        count.set(0);
                    }
                }
            }
            return count.incrementAndGet() <= maxRequests;
        }
    }
}
