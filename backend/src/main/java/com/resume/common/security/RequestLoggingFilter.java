package com.resume.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 请求日志过滤器，注入 trace ID 并记录方法、路径、状态与耗时。
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String MDC_KEY = "traceId";
    /** traceId 最大长度：客户端传入超长值时截断，防止响应头/MDC 被滥用。 */
    private static final int MAX_TRACE_ID_LENGTH = 64;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String traceId = sanitizeTraceId(request.getHeader(TRACE_ID_HEADER));
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        MDC.put(MDC_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);

        long start = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            log.info("{} {} {} {}ms",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    duration);
            MDC.remove(MDC_KEY);
        }
    }

    /**
     * 清洗客户端可控的 X-Trace-Id：仅保留 [A-Za-z0-9-_]，截断到 64 字符；
     * 清洗后为空（含非法字符注入场景）则生成新 ID，防止响应头非法字符触发 Tomcat IAE。
     */
    private String sanitizeTraceId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String cleaned = raw.replaceAll("[^A-Za-z0-9\\-_]", "");
        if (cleaned.length() > MAX_TRACE_ID_LENGTH) {
            cleaned = cleaned.substring(0, MAX_TRACE_ID_LENGTH);
        }
        return cleaned.isBlank() ? null : cleaned;
    }
}
