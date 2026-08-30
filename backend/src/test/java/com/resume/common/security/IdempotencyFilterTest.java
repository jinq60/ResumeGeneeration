package com.resume.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.common.entity.IdempotencyRecord;
import com.resume.common.mapper.IdempotencyRecordMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyFilterTest {

    @Mock
    private IdempotencyRecordMapper mapper;

    @Mock
    private FilterChain chain;

    private IdempotencyFilter filter;

    @BeforeEach
    void setUp() {
        filter = new IdempotencyFilter(mapper, new ObjectMapper());
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    private String scopedKey(String rawKey) {
        return DigestUtils.md5DigestAsHex(("anon:POST:/api/resumes:" + rawKey).getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void shouldPassThroughWhenNoHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/resumes");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(mapper);
    }

    @Test
    void shouldPassThroughForGETRequests() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/resumes");
        request.addHeader("Idempotency-Key", "key-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(mapper);
    }

    @Test
    void shouldPassThroughForAuthPath() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setContextPath("/api");
        request.addHeader("Idempotency-Key", "key-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(mapper);
    }

    @Test
    void shouldReplayCachedResponse() throws Exception {
        IdempotencyRecord record = new IdempotencyRecord();
        record.setIdempotencyKey(scopedKey("key-123"));
        record.setResponseStatus(200);
        record.setResponseContentType(MediaType.APPLICATION_JSON_VALUE);
        record.setResponseBody("{\"code\":200,\"message\":\"ok\",\"data\":{\"id\":\"resume_1\"}}");
        record.setExpiresAt(LocalDateTime.now().plusHours(12));

        when(mapper.selectById(anyString())).thenReturn(record);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/resumes");
        request.addHeader("Idempotency-Key", "key-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        assertEquals(200, response.getStatus());
        assertTrue(response.getContentAsString().contains("resume_1"));
    }

    @Test
    void shouldDeleteExpiredRecordAndPassThrough() throws Exception {
        IdempotencyRecord record = new IdempotencyRecord();
        record.setIdempotencyKey(scopedKey("key-123"));
        record.setExpiresAt(LocalDateTime.now().minusHours(1));

        when(mapper.selectById(anyString())).thenReturn(record);
        when(mapper.deleteById(anyString())).thenReturn(1);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/resumes");
        request.addHeader("Idempotency-Key", "key-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        filter.doFilterInternal(request, response, chain);

        verify(mapper).deleteById(scopedKey("key-123"));
        verify(chain).doFilter(eq(request), any());
    }

    @Test
    void shouldSaveRecordOn2xxSuccess() throws Exception {
        // 确保匿名上下文，避免前序测试残留的 SecurityContext 影响 scopedKey
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        when(mapper.selectById(anyString())).thenReturn(null);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/resumes");
        request.addHeader("Idempotency-Key", "key-456");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        filter.doFilterInternal(request, response, chain);

        // 先以 in-flight 占位插入，成功后回写响应
        ArgumentCaptor<IdempotencyRecord> claimCaptor = ArgumentCaptor.forClass(IdempotencyRecord.class);
        verify(mapper).insert(claimCaptor.capture());
        IdempotencyRecord claimed = claimCaptor.getValue();
        assertNotNull(claimed.getIdempotencyKey());
        assertTrue(claimed.getIdempotencyKey().matches("[a-f0-9]{32}"));
        assertEquals(0, claimed.getResponseStatus());
        assertEquals("POST", claimed.getHttpMethod());
        assertEquals("/api/resumes", claimed.getRequestPath());
        assertNotNull(claimed.getExpiresAt());
        assertTrue(claimed.getExpiresAt().isAfter(LocalDateTime.now()));

        ArgumentCaptor<IdempotencyRecord> completeCaptor = ArgumentCaptor.forClass(IdempotencyRecord.class);
        verify(mapper).updateById(completeCaptor.capture());
        IdempotencyRecord completed = completeCaptor.getValue();
        assertEquals(claimed.getIdempotencyKey(), completed.getIdempotencyKey());
        assertEquals(200, completed.getResponseStatus());
    }

    @Test
    void shouldRejectWith425WhenClaimFailsAndOwnerNeverCompletes() throws Exception {
        // 并发场景：占位被其他请求持有（DuplicateKeyException），且执行者未在等待期内完成
        // → 必须返回 425 拒绝，绝不能降级为直接执行业务
        when(mapper.selectById(anyString())).thenReturn(null);
        when(mapper.insert(any(IdempotencyRecord.class))).thenThrow(new DuplicateKeyException("dup"));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/resumes");
        request.addHeader("Idempotency-Key", "key-dup");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        assertEquals(425, response.getStatus());
        assertTrue(response.getContentAsString().contains("425"));
    }

    @Test
    void scopedKeyIsolatesDifferentUsersWithSameIdempotencyKey() throws Exception {
        // 两个不同用户用同一个 Idempotency-Key 调同一接口，必须被视为独立请求
        // 验证 buildScopedKey 把 userId 拼进去的设计
        FilterChain chain = mock(FilterChain.class);
        // 通过 SecurityContextHolder 注入不同用户
        org.springframework.security.core.context.SecurityContext ctxA =
                org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
        ctxA.setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "user_A", "n/a", java.util.Collections.emptyList()));
        org.springframework.security.core.context.SecurityContextHolder.setContext(ctxA);
        MockHttpServletRequest reqA = post("/resumes");
        reqA.addHeader("Idempotency-Key", "shared-key");
        MockHttpServletResponse resA = new MockHttpServletResponse();
        filter.doFilterInternal(reqA, resA, chain);

        org.springframework.security.core.context.SecurityContext ctxB =
                org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
        ctxB.setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "user_B", "n/a", java.util.Collections.emptyList()));
        org.springframework.security.core.context.SecurityContextHolder.setContext(ctxB);
        MockHttpServletRequest reqB = post("/resumes");
        reqB.addHeader("Idempotency-Key", "shared-key");
        MockHttpServletResponse resB = new MockHttpServletResponse();
        filter.doFilterInternal(reqB, resB, chain);

        // 两个用户都应当继续执行（filter 看到不同的 scoped key）
        verify(chain, times(2)).doFilter(any(), any());
        assertEquals(200, resA.getStatus());
        assertEquals(200, resB.getStatus());
    }

    @Test
    void scopedKeyIsolatesDifferentPathsWithSameIdempotencyKey() throws Exception {
        // 同一用户同一 Idempotency-Key 调两个不同路径，必须被视为独立请求
        FilterChain chain = mock(FilterChain.class);
        org.springframework.security.core.context.SecurityContext ctx =
                org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "user_1", "n/a", java.util.Collections.emptyList()));
        org.springframework.security.core.context.SecurityContextHolder.setContext(ctx);

        MockHttpServletRequest reqA = post("/resumes");
        reqA.addHeader("Idempotency-Key", "shared-key");
        MockHttpServletResponse resA = new MockHttpServletResponse();

        MockHttpServletRequest reqB = post("/avatars/upload");
        reqB.addHeader("Idempotency-Key", "shared-key");
        MockHttpServletResponse resB = new MockHttpServletResponse();

        filter.doFilterInternal(reqA, resA, chain);
        filter.doFilterInternal(reqB, resB, chain);

        verify(chain, times(2)).doFilter(any(), any());
    }

    private MockHttpServletRequest post(String path) {
        return new MockHttpServletRequest("POST", path);
    }
}
