package com.resume.common.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.resume.common.entity.IdempotencyRecord;
import com.resume.common.mapper.IdempotencyRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 周期性清理任务：删除已过期的幂等记录。
 * <p>
 * {@code idempotency_record.expires_at} 在写入时设置为 24 小时后，
 * 此任务每小时扫描一次过期记录并删除，避免表无限增长。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyCleanupScheduler {

    private final IdempotencyRecordMapper idempotencyRecordMapper;

    /**
     * 每小时清理一次过期幂等记录。
     */
    @Scheduled(fixedDelayString = "${app.cleanup.idempotency-interval-ms:3600000}")
    public void cleanupExpiredIdempotencyRecords() {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<IdempotencyRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(IdempotencyRecord::getExpiresAt, now);
        int deleted = idempotencyRecordMapper.delete(wrapper);
        if (deleted > 0) {
            log.info("Cleaned up expired idempotency records: count={}", deleted);
        }
    }
}