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
        ReflectionTestUtils.setField(guard, "ipMaxFailures", 2);
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

    @Test
    void shouldBlockIpAfterIpFailureThreshold() {
        String ip = "203.0.113.7";
        assertFalse(guard.isIpBlocked(ip));

        guard.recordIpFailure(ip);
        assertFalse(guard.isIpBlocked(ip));
        // 达到 IP 维度阈值（2 次）后拒绝
        guard.recordIpFailure(ip);
        assertTrue(guard.isIpBlocked(ip));
    }

    @Test
    void ipDimensionShouldNotLockAccountAndViceVersa() {
        String ip = "198.51.100.5";
        // IP 被封禁不影响任何账号的锁定状态（缓解定向锁号 DoS）
        guard.recordIpFailure(ip);
        guard.recordIpFailure(ip);
        assertTrue(guard.isIpBlocked(ip));
        assertFalse(guard.isLocked("13800000000"));

        // 账号锁定不影响 IP 维度
        guard.recordFailure("13800000000");
        guard.recordFailure("13800000000");
        guard.recordFailure("13800000000");
        assertTrue(guard.isLocked("13800000000"));
        assertFalse(guard.isIpBlocked("192.0.2.9"));
    }

    @Test
    void blankIpShouldBeIgnored() {
        assertFalse(guard.isIpBlocked(null));
        assertFalse(guard.isIpBlocked(""));
        guard.recordIpFailure(null);
        guard.recordIpFailure("");
        // 不抛异常即视为通过
    }
}
