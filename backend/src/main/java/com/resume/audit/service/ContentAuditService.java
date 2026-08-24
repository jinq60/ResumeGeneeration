package com.resume.audit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.resume.audit.dto.AuditItemResponse;
import com.resume.audit.dto.AuditStatsResponse;
import com.resume.audit.entity.ContentAudit;
import com.resume.audit.mapper.ContentAuditMapper;
import com.resume.common.constant.BizConstant;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 内容审核业务服务。
 * <p>
 * 审核记录由业务模块在内容创建时调用 {@link #createForResume} 生成，
 * 管理端通过 {@link #review} 完成通过 / 驳回 / 标记风险操作。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentAuditService {

    public static final String TARGET_TYPE_RESUME = "resume";

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_REJECTED = "rejected";
    public static final String STATUS_WARNING = "warning";

    private final ContentAuditMapper contentAuditMapper;

    /**
     * 简历创建时生成审核记录（初始 pending / low）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void createForResume(String userId, String resumeId, String title) {
        ContentAudit audit = new ContentAudit();
        audit.setTargetType(TARGET_TYPE_RESUME);
        audit.setTargetId(resumeId);
        audit.setTargetTitle(title);
        audit.setUserId(userId);
        audit.setRiskLevel("low");
        audit.setStatus(STATUS_PENDING);
        audit.setDeleted(BizConstant.NOT_DELETED);
        audit.setCreatedAt(LocalDateTime.now());
        audit.setUpdatedAt(LocalDateTime.now());
        contentAuditMapper.insert(audit);
    }

    /**
     * 管理端分页查询。
     */
    public Page<AuditItemResponse> list(int page, int size, String keyword, String status, String riskLevel) {
        LambdaQueryWrapper<ContentAudit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ContentAudit::getDeleted, BizConstant.NOT_DELETED);
        if (StringUtils.isNotBlank(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(ContentAudit::getTargetTitle, kw)
                    .or().like(ContentAudit::getUserId, kw));
        }
        if (StringUtils.isNotBlank(status)) {
            wrapper.eq(ContentAudit::getStatus, status);
        }
        if (StringUtils.isNotBlank(riskLevel)) {
            wrapper.eq(ContentAudit::getRiskLevel, riskLevel);
        }
        wrapper.orderByDesc(ContentAudit::getCreatedAt);

        Page<ContentAudit> pageParam = new Page<>(page, size);
        Page<ContentAudit> result = contentAuditMapper.selectPage(pageParam, wrapper);

        Page<AuditItemResponse> responsePage = new Page<>();
        responsePage.setRecords(result.getRecords().stream().map(AuditItemResponse::from).toList());
        responsePage.setTotal(result.getTotal());
        responsePage.setCurrent(result.getCurrent());
        responsePage.setSize(result.getSize());
        responsePage.setPages(result.getPages());
        return responsePage;
    }

    /**
     * 管理端统计。
     */
    public AuditStatsResponse stats() {
        AuditStatsResponse response = new AuditStatsResponse();
        response.setTotal(countByStatus(null));
        response.setPending(countByStatus(STATUS_PENDING));
        response.setApproved(countByStatus(STATUS_APPROVED));
        response.setRejected(countByStatus(STATUS_REJECTED));
        response.setWarning(countByStatus(STATUS_WARNING));
        response.setTodayReviewed(contentAuditMapper.selectCount(
                new LambdaQueryWrapper<ContentAudit>()
                        .eq(ContentAudit::getDeleted, BizConstant.NOT_DELETED)
                        .isNotNull(ContentAudit::getReviewedAt)
                        .ge(ContentAudit::getReviewedAt, LocalDate.now().atStartOfDay())));
        return response;
    }

    /**
     * 审核操作：通过 / 驳回 / 标记风险。
     */
    @Transactional(rollbackFor = Exception.class)
    public AuditItemResponse review(String reviewerId, String auditId, String action, String note, String riskLevel) {
        ContentAudit audit = contentAuditMapper.selectById(auditId);
        if (audit == null || BizConstant.DELETED.equals(audit.getDeleted())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "审核记录不存在。");
        }
        String nextStatus = switch (action) {
            case "approve" -> STATUS_APPROVED;
            case "reject" -> STATUS_REJECTED;
            case "warning" -> STATUS_WARNING;
            default -> throw new BusinessException(ResultCode.PARAM_INVALID, "审核动作不合法。");
        };
        if ("warning".equals(nextStatus)) {
            String level = StringUtils.isNotBlank(riskLevel) ? riskLevel : "low";
            if (!level.matches("low|medium|high")) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "风险等级不合法。");
            }
            audit.setRiskLevel(level);
        }
        audit.setStatus(nextStatus);
        audit.setReviewerId(reviewerId);
        audit.setReviewNote(note);
        audit.setReviewedAt(LocalDateTime.now());
        audit.setUpdatedAt(LocalDateTime.now());
        contentAuditMapper.updateById(audit);
        log.info("audit reviewed: auditId={}, action={}, reviewerId={}", auditId, action, reviewerId);
        return AuditItemResponse.from(audit);
    }

    private long countByStatus(String status) {
        LambdaQueryWrapper<ContentAudit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ContentAudit::getDeleted, BizConstant.NOT_DELETED);
        if (status != null) {
            wrapper.eq(ContentAudit::getStatus, status);
        }
        return contentAuditMapper.selectCount(wrapper);
    }
}