package com.resume.user.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider();
        String secret = Base64.getEncoder().encodeToString("resume-generation-default-secret-key-must-be-is-32-bytes".getBytes());
        ReflectionTestUtils.setField(provider, "jwtSecret", secret);
        ReflectionTestUtils.setField(provider, "accessTokenExpiration", 3600000L);
        ReflectionTestUtils.setField(provider, "refreshTokenExpiration", 604800000L);
    }

    @Test
    void generateAndValidateAccessToken() {
        String token = provider.generateAccessToken("user_1", false);
        assertNotNull(token);
        assertTrue(provider.validateToken(token));
        assertEquals("user_1", provider.getUserId(token));
    }

    @Test
    void validateToken_shouldRejectMalformedToken() {
        assertFalse(provider.validateToken("not.a.token"));
    }

    @Test
    void validateToken_shouldRejectExpiredToken() throws InterruptedException {
        ReflectionTestUtils.setField(provider, "accessTokenExpiration", 1L);
        String token = provider.generateAccessToken("user_1", false);
        Thread.sleep(10);
        assertFalse(provider.validateToken(token));
    }
}
