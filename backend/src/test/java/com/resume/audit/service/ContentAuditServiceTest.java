package com.resume.audit.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.resume.audit.dto.AuditItemResponse;
import com.resume.audit.entity.ContentAudit;
import com.resume.audit.mapper.ContentAuditMapper;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContentAuditServiceTest {

    @Mock
    private ContentAuditMapper contentAuditMapper;

    private ContentAuditService contentAuditService;

    @BeforeEach
    void setUp() {
        contentAuditService = new ContentAuditService(contentAuditMapper);
    }

    @Test
    void createForResume_shouldInsertPendingLowRisk() {
        when(contentAuditMapper.insert(any(ContentAudit.class))).thenAnswer(inv -> {
            ContentAudit audit = inv.getArgument(0);
            audit.setId("audit_1");
            return 1;
        });

        contentAuditService.createForResume("user_1", "resume_1", "我的简历");

        ArgumentCaptor<ContentAudit> captor = ArgumentCaptor.forClass(ContentAudit.class);
        verify(contentAuditMapper).insert(captor.capture());
        assertEquals("resume", captor.getValue().getTargetType());
        assertEquals("resume_1", captor.getValue().getTargetId());
        assertEquals("pending", captor.getValue().getStatus());
        assertEquals("low", captor.getValue().getRiskLevel());
    }

    @Test
    void list_shouldReturnPagedAudits() {
        ContentAudit audit = new ContentAudit();
        audit.setId("audit_1");
        audit.setTargetTitle("我的简历");
        Page<ContentAudit> page = new Page<>();
        page.setRecords(List.of(audit));
        page.setTotal(1);
        when(contentAuditMapper.selectPage(any(), any())).thenReturn(page);

        Page<AuditItemResponse> result = contentAuditService.list(1, 20, null, "pending", null);

        assertEquals(1, result.getTotal());
        assertEquals("我的简历", result.getRecords().get(0).getTargetTitle());
    }

    @Test
    void review_shouldApprove() {
        ContentAudit audit = new ContentAudit();
        audit.setId("audit_1");
        audit.setStatus("pending");
        when(contentAuditMapper.selectById("audit_1")).thenReturn(audit);

        AuditItemResponse response = contentAuditService.review("admin_1", "audit_1", "approve", "内容合规", null);

        assertEquals("approved", response.getStatus());
        assertEquals("admin_1", response.getReviewerId());
        assertEquals("内容合规", response.getReviewNote());
        assertNotNull(response.getReviewedAt());
    }

    @Test
    void review_shouldReject() {
        ContentAudit audit = new ContentAudit();
        audit.setId("audit_1");
        audit.setStatus("pending");
        when(contentAuditMapper.selectById("audit_1")).thenReturn(audit);

        AuditItemResponse response = contentAuditService.review("admin_1", "audit_1", "reject", "存在隐私信息", null);

        assertEquals("rejected", response.getStatus());
    }

    @Test
    void review_shouldMarkWarningWithRiskLevel() {
        ContentAudit audit = new ContentAudit();
        audit.setId("audit_1");
        audit.setStatus("pending");
        audit.setRiskLevel("low");
        when(contentAuditMapper.selectById("audit_1")).thenReturn(audit);

        AuditItemResponse response = contentAuditService.review("admin_1", "audit_1", "warning", "需关注", "high");

        assertEquals("warning", response.getStatus());
        assertEquals("high", response.getRiskLevel());
    }

    @Test
    void review_shouldRejectInvalidRiskLevel() {
        ContentAudit audit = new ContentAudit();
        audit.setId("audit_1");
        audit.setStatus("pending");
        when(contentAuditMapper.selectById("audit_1")).thenReturn(audit);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> contentAuditService.review("admin_1", "audit_1", "warning", null, "extreme"));
        assertEquals(ResultCode.PARAM_INVALID, ex.getErrorCode());
    }

    @Test
    void review_shouldRejectUnknownAction() {
        ContentAudit audit = new ContentAudit();
        audit.setId("audit_1");
        when(contentAuditMapper.selectById("audit_1")).thenReturn(audit);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> contentAuditService.review("admin_1", "audit_1", "delete", null, null));
        assertEquals(ResultCode.PARAM_INVALID, ex.getErrorCode());
    }

    @Test
    void review_shouldRejectMissingAudit() {
        when(contentAuditMapper.selectById("audit_missing")).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> contentAuditService.review("admin_1", "audit_missing", "approve", null, null));
        assertEquals(ResultCode.RESOURCE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void review_shouldRejectAlreadyApprovedRecord() {
        ContentAudit audit = new ContentAudit();
        audit.setId("audit_1");
        audit.setStatus("approved");
        when(contentAuditMapper.selectById("audit_1")).thenReturn(audit);

        // 终态保护：已通过的记录不允许再次审核（如再驳回）
        BusinessException ex = assertThrows(BusinessException.class,
                () -> contentAuditService.review("admin_1", "audit_1", "reject", "改判", null));
        assertEquals(ResultCode.PARAM_INVALID, ex.getErrorCode());
        verify(contentAuditMapper, never()).updateById(any(ContentAudit.class));
    }

    @Test
    void review_shouldRejectAlreadyRejectedRecord() {
        ContentAudit audit = new ContentAudit();
        audit.setId("audit_1");
        audit.setStatus("rejected");
        when(contentAuditMapper.selectById("audit_1")).thenReturn(audit);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> contentAuditService.review("admin_1", "audit_1", "approve", "翻案", null));
        assertEquals(ResultCode.PARAM_INVALID, ex.getErrorCode());
        verify(contentAuditMapper, never()).updateById(any(ContentAudit.class));
    }
}