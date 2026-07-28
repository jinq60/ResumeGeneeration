package com.resume.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.resume.user.entity.RefreshToken;
import com.resume.user.mapper.RefreshTokenMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 周期性清理任务：物理删除已过期的 refresh_token 记录。
 * <p>
 * 由于 refresh_token 一次性使用，正常刷新流程已删除旧记录，
 * 但用户注销或令牌过期未刷新时，记录会留存，需要靠此任务清理。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenMapper refreshTokenMapper;

    /**
     * 每小时清理一次过期的 refresh token 记录。
     */
    @Scheduled(fixedDelayString = "${app.cleanup.refresh-token-interval-ms:3600000}")
    public void cleanupExpiredRefreshTokens() {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<RefreshToken> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(RefreshToken::getExpiresAt, now);
        int deleted = refreshTokenMapper.delete(wrapper);
        if (deleted > 0) {
            log.info("Cleaned up expired refresh tokens: count={}", deleted);
        }
    }
}