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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 简易限流过滤器。
 * <p>
 * 委托给 {@link RateLimiter} 接口实现（默认内存实现，可替换为 Redis）。
 * 底层计数器使用 {@link java.util.concurrent.ConcurrentHashMap#compute} 原子地完成窗口创建与计数，
 * 避免 cleanup 与窗口更新之间出现竞态。
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
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String ctx = request.getContextPath() == null ? "" : request.getContextPath();
        String path = uri.startsWith(ctx) ? uri.substring(ctx.length()) : uri;
        return path.startsWith("/actuator/health")
                || path.startsWith("/actuator/prometheus")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-resources");
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
            response.setHeader("Retry-After", String.valueOf(windowMs / 1000));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(objectMapper.writeValueAsString(
                    R.error(ResultCode.RATE_LIMITED, "请求过于频繁，请稍后再试。")));
        }
    }

    private String resolveKey(HttpServletRequest request) {
        // 已认证请求按 userId 计数（不同用户互不影响）；匿名请求按 IP 计数。
        // 不直接信任 X-Forwarded-For，防止客户端伪造 IP 绕过限流。
        // 如需获取真实客户端 IP，应在可信反向代理后统一部署，由网关统一注入并校验。
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String userId
                && StringUtils.hasText(userId)) {
            return "rate:user:" + userId;
        }
        return "rate:ip:" + request.getRemoteAddr();
    }
}
