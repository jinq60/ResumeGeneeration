package com.resume.common.service;

import com.resume.common.entity.AuditLog;
import com.resume.common.mapper.AuditLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 审计日志服务：记录登录/注册/删除/导出等敏感操作。
 * <p>
 * 写入失败只记录日志，不影响主业务流程。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogMapper auditLogMapper;

    /**
     * 记录一条审计日志。
     *
     * @param userId   操作人 ID（可空）
     * @param action   动作标识（见 V7 迁移注释的枚举）
     * @param targetId 操作对象 ID（可空）
     * @param detail   补充描述（可空）
     */
    public void record(String userId, String action, String targetId, String detail) {
        try {
            AuditLog entry = new AuditLog();
            entry.setUserId(userId);
            entry.setAction(action);
            entry.setTargetId(targetId);
            entry.setDetail(detail == null || detail.length() <= 512 ? detail : detail.substring(0, 512));
            entry.setCreatedAt(LocalDateTime.now());
            auditLogMapper.insert(entry);
        } catch (Exception e) {
            log.warn("Failed to record audit log: action={}, userId={}", action, userId, e);
        }
    }
}
