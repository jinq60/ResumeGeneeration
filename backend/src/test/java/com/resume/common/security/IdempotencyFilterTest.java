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
        assertEquals(scopedKey("key-456"), claimed.getIdempotencyKey());
        assertEquals(0, claimed.getResponseStatus());
        assertEquals("POST", claimed.getHttpMethod());
        assertEquals("/api/resumes", claimed.getRequestPath());
        assertNotNull(claimed.getExpiresAt());
        assertTrue(claimed.getExpiresAt().isAfter(LocalDateTime.now()));

        ArgumentCaptor<IdempotencyRecord> completeCaptor = ArgumentCaptor.forClass(IdempotencyRecord.class);
        verify(mapper).updateById(completeCaptor.capture());
        IdempotencyRecord completed = completeCaptor.getValue();
        assertEquals(scopedKey("key-456"), completed.getIdempotencyKey());
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
}
