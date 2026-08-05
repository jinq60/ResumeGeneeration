package com.resume.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录失败锁定（内存实现，单实例部署适用）。
 * <p>
 * 以账号为粒度：连续失败达到上限后锁定一段时间，防止密码爆破。
 * 成功登录或锁定到期后自动复位。
 * </p>
 */
@Slf4j
@Component
public class LoginAttemptGuard {

    private final Map<String, AttemptState> states = new ConcurrentHashMap<>();

    @Value("${app.auth.login.max-failures:5}")
    private int maxFailures;

    @Value("${app.auth.login.lockout-minutes:15}")
    private long lockoutMinutes;

    /**
     * 账号是否处于锁定状态。
     */
    public boolean isLocked(String account) {
        if (account == null) {
            return false;
        }
        AttemptState state = states.get(account);
        if (state == null) {
            return false;
        }
        if (state.lockUntil != null && System.currentTimeMillis() >= state.lockUntil) {
            states.remove(account);
            return false;
        }
        return state.failures >= maxFailures;
    }

    /**
     * 记录一次登录失败；达到上限时触发锁定。
     */
    public void recordFailure(String account) {
        if (account == null) {
            return;
        }
        states.compute(account, (k, state) -> {
            AttemptState next = state == null ? new AttemptState() : state;
            next.failures++;
            if (next.failures >= maxFailures && next.lockUntil == null) {
                next.lockUntil = System.currentTimeMillis() + lockoutMinutes * 60_000L;
                log.warn("Login account locked: account={}, failures={}, lockoutMinutes={}",
                        k, next.failures, lockoutMinutes);
            }
            return next;
        });
    }

    /**
     * 登录成功后复位。
     */
    public void reset(String account) {
        if (account != null) {
            states.remove(account);
        }
    }

    private static class AttemptState {
        int failures = 0;
        Long lockUntil = null;
    }
}
