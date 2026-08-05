package com.resume.user.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class GuestAccountGuardTest {

    private GuestAccountGuard guard = new GuestAccountGuard();

    @Test
    void shouldAllowWithinDailyLimit() {
        ReflectionTestUtils.setField(guard, "maxPerIpPerDay", 3);

        assertTrue(guard.tryAcquire("1.2.3.4"));
        assertTrue(guard.tryAcquire("1.2.3.4"));
        assertTrue(guard.tryAcquire("1.2.3.4"));
        assertFalse(guard.tryAcquire("1.2.3.4"));
        // 不同 IP 互不影响
        assertTrue(guard.tryAcquire("5.6.7.8"));
    }

    @Test
    void shouldAllowDifferentIpIndependently() {
        ReflectionTestUtils.setField(guard, "maxPerIpPerDay", 1);

        assertTrue(guard.tryAcquire("10.0.0.1"));
        assertFalse(guard.tryAcquire("10.0.0.1"));
        assertTrue(guard.tryAcquire("10.0.0.2"));
    }

    @Test
    void shouldHandleBlankIp() {
        ReflectionTestUtils.setField(guard, "maxPerIpPerDay", 1);

        assertTrue(guard.tryAcquire(""));
        assertFalse(guard.tryAcquire(null));
    }
}
