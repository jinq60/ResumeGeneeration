package com.resume.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.common.constant.ResultCode;
import com.resume.common.entity.IdempotencyRecord;
import com.resume.common.entity.R;
import com.resume.common.mapper.IdempotencyRecordMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.DigestUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 幂等性过滤器（注册于 Spring Security 链内、JWT 认证之后）。
 * <p>
 * 对携带 {@code Idempotency-Key} 请求头的 POST/PUT/PATCH/DELETE 请求：
 * <ul>
 *   <li>缓存键包含 用户ID + 方法 + 路径，防止跨用户/跨路径响应投毒；</li>
 *   <li>通过唯一主键原子占位（insert 成功者为唯一执行者），消除并发下重复执行副作用；</li>
 *   <li>并发重复请求通过 {@link CompletableFuture} 精准等待执行者完成，避免固定间隔轮询；</li>
 *   <li>/auth/** 返回凭证，禁止缓存。</li>
 * </ul>
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final String HEADER_KEY = "Idempotency-Key";
    private static final int TTL_HOURS = 24;
    private static final Set<String> IDEMPOTENT_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final long WAIT_TIMEOUT_MS = 5_000;
    private static final int STATUS_IN_FLIGHT = 0;

    private final IdempotencyRecordMapper idempotencyRecordMapper;
    private final ObjectMapper objectMapper;

    /**
     * 进行中请求的完成信号：key = scopedKey，value = 执行者完成时被 complete 的 future。
     */
    private final Map<String, CompletableFuture<IdempotencyRecord>> inFlight = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String method = request.getMethod().toUpperCase();
        if (!IDEMPOTENT_METHODS.contains(method)) {
            chain.doFilter(request, response);
            return;
        }

        String rawKey = request.getHeader(HEADER_KEY);
        if (rawKey == null || rawKey.isBlank() || rawKey.length() > 64) {
            chain.doFilter(request, response);
            return;
        }

        // 认证接口返回凭证，禁止缓存
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
        if (uri.startsWith(contextPath + "/auth/") || uri.startsWith("/auth/")) {
            chain.doFilter(request, response);
            return;
        }

        String userId = resolveUserId();
        String scopedKey = buildScopedKey(userId, method, request.getRequestURI(), rawKey);

        IdempotencyRecord existing = idempotencyRecordMapper.selectById(scopedKey);
        if (existing != null) {
            if (existing.getExpiresAt() != null && existing.getExpiresAt().isBefore(LocalDateTime.now())) {
                // 过期记录（无论是否完成）删除后重新占位
                idempotencyRecordMapper.deleteById(scopedKey);
            } else if (existing.getResponseStatus() != null
                    && existing.getResponseStatus() > STATUS_IN_FLIGHT) {
                replay(existing, response);
                return;
            } else {
                // 其他请求正在执行同一键的请求：等待其完成并返回结果
                IdempotencyRecord completed = waitForCompletion(scopedKey);
                if (completed != null) {
                    replay(completed, response);
                    return;
                }
            }
        }

        boolean owner = claim(scopedKey, userId, request);
        if (!owner) {
            // 未抢到占位（并发重复请求）：等待执行者完成后重放其响应；
            // 等待超时必须拒绝执行而非降级放行，否则同一 key 的副作用会被重复执行
            // （双扣 AI 配额、重复建任务等）。返回 425 Too Early 让客户端安全重试。
            IdempotencyRecord completed = waitForCompletion(scopedKey);
            if (completed != null) {
                replay(completed, response);
                return;
            }
            log.warn("Idempotency wait timeout without owner completion: key={}", scopedKey);
            respondConflict(response);
            return;
        }

        ContentCachingResponseWrapper wrapped = new ContentCachingResponseWrapper(response);
        IdempotencyRecord completedRecord = null;
        try {
            chain.doFilter(request, wrapped);

            int status = wrapped.getStatus();
            if (HttpStatus.valueOf(status).is2xxSuccessful()) {
                if (owner) {
                    completedRecord = complete(scopedKey, status, wrapped);
                }
            } else if (owner) {
                // 失败请求不缓存，释放占位以允许重试
                idempotencyRecordMapper.deleteById(scopedKey);
            }
        } finally {
            // 无论成败都唤醒等待方，避免其因 future 永不完成而阻塞到超时
            if (owner) {
                completeInFlight(scopedKey, completedRecord);
            }
            wrapped.copyBodyToResponse();
        }
    }

    private void replay(IdempotencyRecord record, HttpServletResponse response) throws IOException {
        log.debug("Idempotency key replay: key={}", record.getIdempotencyKey());
        response.setStatus(record.getResponseStatus());
        response.setContentType(record.getResponseContentType());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(record.getResponseBody());
    }

    /**
     * 同一幂等键的请求仍在处理且等待超时：返回 425，绝不放行执行业务。
     */
    private void respondConflict(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_EARLY.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(
                R.error(ResultCode.IDEMPOTENCY_CONFLICT,
                        "相同请求正在处理中，请稍后重试（Idempotency-Key 冲突）。")));
    }

    private IdempotencyRecord waitForCompletion(String scopedKey) {
        CompletableFuture<IdempotencyRecord> future = inFlight.get(scopedKey);
        if (future == null) {
            return null;
        }
        try {
            return future.get(WAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException e) {
            return null;
        }
    }

    private boolean claim(String scopedKey, String userId, HttpServletRequest request) {
        IdempotencyRecord record = new IdempotencyRecord();
        record.setIdempotencyKey(scopedKey);
        record.setUserId(userId);
        record.setHttpMethod(request.getMethod());
        record.setRequestPath(request.getRequestURI());
        record.setResponseStatus(STATUS_IN_FLIGHT);
        record.setResponseBody("");
        record.setResponseContentType(MediaType.APPLICATION_JSON_VALUE);
        record.setCreatedAt(LocalDateTime.now());
        record.setExpiresAt(LocalDateTime.now().plusHours(TTL_HOURS));
        try {
            idempotencyRecordMapper.insert(record);
            // 占位成功后立即注册完成信号，缩小并发等待方拿不到 future 的窗口
            inFlight.put(scopedKey, new CompletableFuture<>());
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    private IdempotencyRecord complete(String scopedKey, int status, ContentCachingResponseWrapper wrapped) {
        try {
            byte[] body = wrapped.getContentAsByteArray();
            IdempotencyRecord record = new IdempotencyRecord();
            record.setIdempotencyKey(scopedKey);
            record.setResponseStatus(status);
            record.setResponseBody(new String(body, StandardCharsets.UTF_8));
            record.setResponseContentType(
                    wrapped.getContentType() != null ? wrapped.getContentType() : MediaType.APPLICATION_JSON_VALUE);
            idempotencyRecordMapper.updateById(record);
            return record;
        } catch (Exception e) {
            log.warn("Failed to save idempotency record for key={}", scopedKey, e);
            idempotencyRecordMapper.deleteById(scopedKey);
            return null;
        }
    }

    private void completeInFlight(String scopedKey, IdempotencyRecord record) {
        CompletableFuture<IdempotencyRecord> future = inFlight.remove(scopedKey);
        if (future != null) {
            future.complete(record);
        }
    }

    private String resolveUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String userId) {
            return userId;
        }
        return null;
    }

    private String buildScopedKey(String userId, String method, String path, String rawKey) {
        String raw = (userId == null ? "anon" : userId) + ":" + method + ":" + path + ":" + rawKey;
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }
}
