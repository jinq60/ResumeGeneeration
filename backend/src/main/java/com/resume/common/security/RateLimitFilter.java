package com.resume.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.common.constant.ResultCode;
import com.resume.common.entity.R;
import com.resume.common.service.RateLimiter;
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

/**
 * 简易限流过滤器。
 * <p>
 * 委托给 {@link RateLimiter} 接口实现（默认内存实现，可替换为 Redis）。
 * </p>
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter rateLimiter;
    private final ObjectMapper objectMapper;
    private final int maxRequests;
    private final long windowMs;

    public RateLimitFilter(
            RateLimiter rateLimiter,
            ObjectMapper objectMapper,
            @Value("${app.rate-limit.max-requests:60}") int maxRequests,
            @Value("${app.rate-limit.window-seconds:60}") long windowSeconds) {
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
        this.maxRequests = maxRequests;
        this.windowMs = windowSeconds * 1000L;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String key = resolveKey(request);

        if (rateLimiter.tryAcquire(key, maxRequests, windowMs)) {
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
        return "rate:" + request.getRemoteAddr();
    }
}
