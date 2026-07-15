package com.resume.user.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class PasswordEncoderTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void encode_shouldProduceDifferentHashes() {
        String raw = "123456";
        String encoded1 = passwordEncoder.encode(raw);
        String encoded2 = passwordEncoder.encode(raw);

        assertNotEquals(encoded1, encoded2);
        assertTrue(passwordEncoder.matches(raw, encoded1));
        assertTrue(passwordEncoder.matches(raw, encoded2));
    }

    @Test
    void matches_shouldRejectWrongPassword() {
        String encoded = passwordEncoder.encode("123456");
        assertFalse(passwordEncoder.matches("wrong", encoded));
    }
}
