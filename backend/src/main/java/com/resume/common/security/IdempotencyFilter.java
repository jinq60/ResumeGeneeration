package com.resume.common.security;

import com.resume.common.entity.IdempotencyRecord;
import com.resume.common.mapper.IdempotencyRecordMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * 幂等性过滤器。
 * <p>
 * 对携带 {@code Idempotency-Key} 请求头的 POST 请求，在首次成功（2xx）时将响应缓存 24 小时；
 * 重复请求直接返回缓存结果，避免重复创建资源。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final String HEADER_KEY = "Idempotency-Key";
    private static final int TTL_HOURS = 24;

    private final IdempotencyRecordMapper idempotencyRecordMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String key = request.getHeader(HEADER_KEY);
        if (key == null || key.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        IdempotencyRecord existing = idempotencyRecordMapper.selectById(key);
        if (existing != null) {
            if (existing.getExpiresAt().isAfter(LocalDateTime.now())) {
                log.debug("Idempotency key replay: key={}", key);
                response.setStatus(existing.getResponseStatus());
                response.setContentType(existing.getResponseContentType());
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.getWriter().write(existing.getResponseBody());
                return;
            }
            idempotencyRecordMapper.deleteById(key);
        }

        ContentCachingResponseWrapper wrapped = new ContentCachingResponseWrapper(response);
        chain.doFilter(request, wrapped);

        int status = wrapped.getStatus();
        if (HttpStatus.valueOf(status).is2xxSuccessful()) {
            try {
                byte[] body = wrapped.getContentAsByteArray();
                String responseBody = new String(body, StandardCharsets.UTF_8);

                IdempotencyRecord record = new IdempotencyRecord();
                record.setIdempotencyKey(key);
                record.setHttpMethod(request.getMethod());
                record.setRequestPath(request.getRequestURI());
                record.setResponseStatus(status);
                record.setResponseBody(responseBody);
                record.setResponseContentType(
                        wrapped.getContentType() != null ? wrapped.getContentType() : MediaType.APPLICATION_JSON_VALUE);
                record.setCreatedAt(LocalDateTime.now());
                record.setExpiresAt(LocalDateTime.now().plusHours(TTL_HOURS));
                idempotencyRecordMapper.insert(record);
                log.debug("Idempotency key saved: key={}", key);
            } catch (Exception e) {
                log.warn("Failed to save idempotency record for key={}", key, e);
            }
        }

        wrapped.copyBodyToResponse();
    }
}
