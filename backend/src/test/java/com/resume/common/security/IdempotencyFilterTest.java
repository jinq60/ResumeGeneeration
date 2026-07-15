package com.resume.common.security;

import com.resume.common.entity.IdempotencyRecord;
import com.resume.common.mapper.IdempotencyRecordMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
        filter = new IdempotencyFilter(mapper);
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
    void shouldReplayCachedResponse() throws Exception {
        IdempotencyRecord record = new IdempotencyRecord();
        record.setIdempotencyKey("key-123");
        record.setResponseStatus(200);
        record.setResponseContentType(MediaType.APPLICATION_JSON_VALUE);
        record.setResponseBody("{\"code\":200,\"message\":\"ok\",\"data\":{\"id\":\"resume_1\"}}");
        record.setExpiresAt(LocalDateTime.now().plusHours(12));

        when(mapper.selectById("key-123")).thenReturn(record);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/resumes");
        request.addHeader("Idempotency-Key", "key-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        assertEquals(200, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8", response.getContentType());
        assertTrue(response.getContentAsString().contains("resume_1"));
    }

    @Test
    void shouldDeleteExpiredRecordAndPassThrough() throws Exception {
        IdempotencyRecord record = new IdempotencyRecord();
        record.setIdempotencyKey("key-123");
        record.setExpiresAt(LocalDateTime.now().minusHours(1));

        when(mapper.selectById("key-123")).thenReturn(record);
        when(mapper.deleteById("key-123")).thenReturn(1);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/resumes");
        request.addHeader("Idempotency-Key", "key-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        filter.doFilterInternal(request, response, chain);

        verify(mapper).deleteById("key-123");
        verify(chain).doFilter(eq(request), any());
    }

    @Test
    void shouldSaveRecordOn2xxSuccess() throws Exception {
        when(mapper.selectById("key-456")).thenReturn(null);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/resumes");
        request.addHeader("Idempotency-Key", "key-456");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        filter.doFilterInternal(request, response, chain);

        ArgumentCaptor<IdempotencyRecord> captor = ArgumentCaptor.forClass(IdempotencyRecord.class);
        verify(mapper).insert(captor.capture());
        IdempotencyRecord saved = captor.getValue();
        assertEquals("key-456", saved.getIdempotencyKey());
        assertEquals("POST", saved.getHttpMethod());
        assertEquals("/api/resumes", saved.getRequestPath());
        assertNotNull(saved.getExpiresAt());
        assertTrue(saved.getExpiresAt().isAfter(LocalDateTime.now()));
    }
}
