package com.resume.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.common.service.RateLimiter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RateLimitFilterTest {

    private RateLimiter rateLimiter;
    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        rateLimiter = mock(RateLimiter.class);
        filter = new RateLimitFilter(rateLimiter, new ObjectMapper(), 60, 60);
        when(rateLimiter.tryAcquire(anyString(), eq(60), eq(60_000L))).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldUseUserIdKeyWhenAuthenticated() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user_1", null));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/resumes");
        request.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(rateLimiter).tryAcquire("rate:user:user_1", 60, 60_000L);
        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldUseIpKeyWhenAnonymous() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/templates");
        request.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(rateLimiter).tryAcquire("rate:ip:10.0.0.1", 60, 60_000L);
        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldRejectWith429WhenLimitExceeded() throws Exception {
        when(rateLimiter.tryAcquire(anyString(), eq(60), eq(60_000L))).thenReturn(false);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/resumes");
        request.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(429, response.getStatus());
        verifyNoInteractions(chain);
    }
}
