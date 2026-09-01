package com.resume.common.service;

import com.resume.common.entity.AuditLog;
import com.resume.common.mapper.AuditLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 审计日志服务测试：写入失败不能抛出去影响主业务。
 */
class AuditLogServiceTest {

    private AuditLogMapper mapper;
    private AuditLogService service;

    @BeforeEach
    void setUp() {
        mapper = mock(AuditLogMapper.class);
        service = new AuditLogService(mapper);
    }

    @Test
    void record_writesEntryWithTrimmedDetail() {
        service.record("user_1", "LOGIN", null, "ok");
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(mapper).insert(captor.capture());
        AuditLog entry = captor.getValue();
        assertEquals("user_1", entry.getUserId());
        assertEquals("LOGIN", entry.getAction());
        assertEquals("ok", entry.getDetail());
        assertNotNull(entry.getCreatedAt());
    }

    @Test
    void record_truncatesLongDetail() {
        String huge = "x".repeat(2000);
        service.record("user_1", "DELETE", "resume_x", huge);
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(mapper).insert(captor.capture());
        assertEquals(512, captor.getValue().getDetail().length());
    }

    @Test
    void record_handlesNullDetail() {
        service.record("user_1", "EXPORT", "resume_x", null);
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(mapper).insert(captor.capture());
        assertEquals(null, captor.getValue().getDetail());
    }

    @Test
    void record_swallowsMapperException() {
        doThrow(new RuntimeException("db down")).when(mapper).insert(any(AuditLog.class));
        // 主业务调用方不应被审计失败打断
        service.record("user_1", "LOGIN", null, "ok");
        // 不应向上抛
        verify(mapper).insert(any(AuditLog.class));
    }
}
