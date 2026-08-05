package com.resume.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptGuardTest {

    private LoginAttemptGuard guard = new LoginAttemptGuard();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(guard, "maxFailures", 3);
        ReflectionTestUtils.setField(guard, "lockoutMinutes", 15L);
    }

    @Test
    void shouldLockAfterMaxFailuresAndResetAfterLockout() throws Exception {
        assertFalse(guard.isLocked("13800000000"));

        guard.recordFailure("13800000000");
        guard.recordFailure("13800000000");
        assertFalse(guard.isLocked("13800000000"));

        guard.recordFailure("13800000000");
        assertTrue(guard.isLocked("13800000000"));

        // 模拟锁定到期
        Object state = ReflectionTestUtils.getField(guard, "states");
        java.util.Map<?, ?> map = (java.util.Map<?, ?>) state;
        Object entry = map.get("13800000000");
        ReflectionTestUtils.setField(entry, "lockUntil", System.currentTimeMillis() - 1000);
        assertFalse(guard.isLocked("13800000000"));
    }

    @Test
    void shouldResetAfterSuccessfulLogin() {
        guard.recordFailure("13800000000");
        guard.recordFailure("13800000000");
        guard.recordFailure("13800000000");
        assertTrue(guard.isLocked("13800000000"));

        guard.reset("13800000000");
        assertFalse(guard.isLocked("13800000000"));
    }

    @Test
    void shouldNotAffectOtherAccounts() {
        guard.recordFailure("13800000000");
        guard.recordFailure("13800000000");
        guard.recordFailure("13800000000");
        assertTrue(guard.isLocked("13800000000"));
        assertFalse(guard.isLocked("13900000000"));
    }
}
