package com.resume.ai.service;

import com.resume.ai.entity.AiDailyQuota;
import com.resume.ai.mapper.AiDailyQuotaMapper;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiDailyQuotaServiceTest {

    @Mock
    private AiDailyQuotaMapper aiDailyQuotaMapper;

    @InjectMocks
    private AiDailyQuotaService service;

    private AiDailyQuota buildQuota(int usedCount) {
        AiDailyQuota quota = new AiDailyQuota();
        quota.setId("quota_1");
        quota.setUserId("user_1");
        quota.setFeatureKey("resume-writing");
        quota.setQuotaDate("20260806");
        quota.setUsedCount(usedCount);
        quota.setDeleted(0);
        return quota;
    }

    @Test
    void consume_shouldInsertFirstRecord() {
        when(aiDailyQuotaMapper.selectOne(any())).thenReturn(null);
        when(aiDailyQuotaMapper.insert(any())).thenReturn(1);

        service.consume("user_1", "resume-writing", 3);

        verify(aiDailyQuotaMapper).insert(any());
    }

    @Test
    void consume_shouldIncrementWithinLimit() {
        when(aiDailyQuotaMapper.selectOne(any())).thenReturn(buildQuota(1));
        when(aiDailyQuotaMapper.update(any(), any())).thenReturn(1);

        assertDoesNotThrow(() -> service.consume("user_1", "resume-writing", 3));
        verify(aiDailyQuotaMapper).update(any(), any());
    }

    @Test
    void consume_shouldRejectWhenLimitReached() {
        when(aiDailyQuotaMapper.selectOne(any())).thenReturn(buildQuota(3));
        when(aiDailyQuotaMapper.update(any(), any())).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.consume("user_1", "resume-writing", 3));
        assertEquals(ResultCode.AI_DAILY_QUOTA_EXCEEDED, ex.getErrorCode());
    }

    @Test
    void consume_shouldRejectWhenLimitIsZero() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.consume("user_1", "resume-writing", 0));
        assertEquals(ResultCode.AI_DAILY_QUOTA_EXCEEDED, ex.getErrorCode());
    }

    @Test
    void consume_shouldRetryViaUpdateWhenInsertRaces() {
        when(aiDailyQuotaMapper.selectOne(any())).thenReturn(null);
        when(aiDailyQuotaMapper.insert(any()))
                .thenThrow(new DuplicateKeyException("duplicate key"));
        when(aiDailyQuotaMapper.update(any(), any())).thenReturn(1);

        assertDoesNotThrow(() -> service.consume("user_1", "resume-writing", 3));
        verify(aiDailyQuotaMapper).update(any(), any());
    }
}
