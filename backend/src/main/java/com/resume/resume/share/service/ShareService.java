package com.resume.resume.share.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.resume.common.constant.BizConstant;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.common.service.ResumeRenderService;
import com.resume.resume.entity.Resume;
import com.resume.resume.mapper.ResumeMapper;
import com.resume.resume.service.ResumeService;
import com.resume.resume.share.dto.ShareResponse;
import com.resume.resume.share.entity.ResumeShare;
import com.resume.resume.share.mapper.ResumeShareMapper;
import com.resume.template.entity.Template;
import com.resume.template.service.TemplateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * 简历公开分享服务。
 * <p>
 * token 为 32 字节随机数（不可枚举）；关闭后立即失效；
 * 分享页为只读服务端渲染 HTML，不依赖登录态。
 * </p>
 */
@Slf4j
@Service
public class ShareService {

    public static final String SHARE_STATUS_ACTIVE = "active";
    public static final String SHARE_STATUS_REVOKED = "revoked";

    private final ResumeShareMapper resumeShareMapper;
    private final ResumeMapper resumeMapper;
    private final ResumeRenderService resumeRenderService;
    private final TemplateService templateService;
    /**
     * 用 @Lazy 打破与 ResumeService 的循环（ResumeService -> ShareService.revokeByResume）。
     * 公共 renderSharePage(token) 仍直接用 ResumeMapper（无 userId 上下文）。
     */
    @Lazy
    private final ResumeService resumeService;

    private final SecureRandom secureRandom = new SecureRandom();
    private final java.util.concurrent.ConcurrentHashMap<String, Object> createLocks = new java.util.concurrent.ConcurrentHashMap<>();

    public ShareService(ResumeShareMapper resumeShareMapper, ResumeMapper resumeMapper,
                        ResumeRenderService resumeRenderService, TemplateService templateService,
                        @Lazy ResumeService resumeService) {
        this.resumeShareMapper = resumeShareMapper;
        this.resumeMapper = resumeMapper;
        this.resumeRenderService = resumeRenderService;
        this.templateService = templateService;
        this.resumeService = resumeService;
    }

    /**
     * 创建或轮换分享（同一简历只保留一条有效分享）。
     *
     * @param hideContact 分享页是否隐藏联系方式
     * @param expiresAt   过期时间；为空表示永久有效
     */
    @Transactional(rollbackFor = Exception.class)
    public ShareResponse createShare(String userId, String resumeId, boolean hideContact, LocalDateTime expiresAt) {
        validateExpiresAt(expiresAt);
        resumeServiceAccessCheck(userId, resumeId);
        Object lock = createLocks.computeIfAbsent(resumeId, k -> new Object());
        synchronized (lock) {
            // 关闭旧的分享（锁内串行化，避免并发双 active）
            revokeShareRecords(userId, resumeId);
            ResumeShare share = new ResumeShare();
            share.setResumeId(resumeId);
            share.setUserId(userId);
            share.setToken(generateToken());
            share.setStatus(SHARE_STATUS_ACTIVE);
            share.setHideContact(hideContact);
            share.setExpiresAt(expiresAt);
            share.setDeleted(BizConstant.NOT_DELETED);
            share.setCreatedAt(LocalDateTime.now());
            share.setUpdatedAt(LocalDateTime.now());
            try {
                resumeShareMapper.insert(share);
            } catch (org.springframework.dao.DuplicateKeyException e) {
                // token 极小概率碰撞，重试一次
                log.warn("Share token collision, retry: resumeId={}", resumeId);
                share.setToken(generateToken());
                resumeShareMapper.insert(share);
            }
            log.info("Resume share created: resumeId={}, shareId={}, hideContact={}, expiresAt={}",
                    resumeId, share.getId(), hideContact, expiresAt);
            return toResponse(share);
        }
    }

    private void validateExpiresAt(LocalDateTime expiresAt) {
        if (expiresAt != null && !expiresAt.isAfter(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "过期时间必须晚于当前时间。");
        }
    }

    /**
     * 查询当前有效分享（无则返回 null）。
     */
    public ShareResponse getShare(String userId, String resumeId) {
        resumeServiceAccessCheck(userId, resumeId);
        ResumeShare share = findActiveShare(userId, resumeId);
        return share == null ? null : toResponse(share);
    }

    /**
     * 关闭分享。
     */
    @Transactional(rollbackFor = Exception.class)
    public void revokeShare(String userId, String resumeId) {
        resumeServiceAccessCheck(userId, resumeId);
        revokeShareRecords(userId, resumeId);
    }

    /**
     * 按 token 渲染公开只读页面。
     */
    public String renderSharePage(String token) {
        ResumeShare share = findByToken(token);
        if (share == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "分享不存在或已失效。");
        }
        if (share.getExpiresAt() != null && share.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "分享已过期。");
        }
        Resume resume = resumeMapper.selectById(share.getResumeId());
        if (resume == null || BizConstant.DELETED.equals(resume.getDeleted())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "简历不存在或已被删除。");
        }
        // 渲染场景容忍 inactive/deleted 模板：模板被删后历史分享页仍需可渲染
        Template template = templateService.getTemplateEntityForRender(resume.getTemplateId());
        String body = Boolean.TRUE.equals(share.getHideContact())
                ? resumeRenderService.render(resume, template, ResumeRenderService.RenderOptions.withHiddenContact())
                : resumeRenderService.render(resume, template);
        return wrapSharePage(resume.getTitle(), body);
    }

    /**
     * 简历删除时级联关闭其分享。
     */
    @Transactional(rollbackFor = Exception.class)
    public void revokeByResume(String resumeId) {
        if (StringUtils.isBlank(resumeId)) {
            return;
        }
        // 逻辑删除字段必须用 UpdateWrapper.set 显式写入：实体方式会被 MP 从 SET 子句排除导致静默失效
        LambdaUpdateWrapper<ResumeShare> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ResumeShare::getResumeId, resumeId)
                .set(ResumeShare::getStatus, SHARE_STATUS_REVOKED)
                .set(ResumeShare::getRevokedAt, LocalDateTime.now())
                .set(ResumeShare::getDeleted, BizConstant.DELETED)
                .set(ResumeShare::getUpdatedAt, LocalDateTime.now());
        resumeShareMapper.update(null, wrapper);
    }

    /**
     * 分享页壳：noindex + 简洁页脚。
     */
    private String wrapSharePage(String title, String body) {
        String safeTitle = StringUtils.defaultString(title, "简历分享");
        return "<!DOCTYPE html>\n"
                + "<html lang=\"zh-CN\">\n"
                + "<head>\n"
                + "  <meta charset=\"UTF-8\">\n"
                + "  <meta name=\"robots\" content=\"noindex,nofollow\">\n"
                + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n"
                + "  <title>" + escapeHtml(safeTitle) + " - 简历分享</title>\n"
                + "</head>\n"
                + "<body style=\"margin:0;background:#fafbfc;font-family:-apple-system,'Segoe UI','Noto Sans SC','Microsoft YaHei',sans-serif;\">\n"
                + "  <div style=\"max-width:210mm;margin:24px auto;box-shadow:0 2px 12px rgba(0,0,0,0.08);\">\n"
                + "    " + body + "\n"
                + "  </div>\n"
                + "  <div style=\"text-align:center;padding:16px 0 32px;color:#6b7280;font-size:12px;\">\n"
                + "    由 <a href=\"/\" style=\"color:#0057c2;text-decoration:none;\">智能简历</a> 生成 · 本页面为公开只读预览\n"
                + "  </div>\n"
                + "</body>\n"
                + "</html>";
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private ResumeShare findActiveShare(String userId, String resumeId) {
        LambdaQueryWrapper<ResumeShare> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResumeShare::getUserId, userId)
                .eq(ResumeShare::getResumeId, resumeId)
                .eq(ResumeShare::getStatus, SHARE_STATUS_ACTIVE)
                .eq(ResumeShare::getDeleted, BizConstant.NOT_DELETED)
                .orderByDesc(ResumeShare::getCreatedAt)
                .last("LIMIT 1");
        return resumeShareMapper.selectOne(wrapper);
    }

    private ResumeShare findByToken(String token) {
        LambdaQueryWrapper<ResumeShare> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResumeShare::getToken, token)
                .eq(ResumeShare::getStatus, SHARE_STATUS_ACTIVE)
                .eq(ResumeShare::getDeleted, BizConstant.NOT_DELETED)
                .last("LIMIT 1");
        return resumeShareMapper.selectOne(wrapper);
    }

    private void revokeShareRecords(String userId, String resumeId) {
        // 逻辑删除字段必须用 UpdateWrapper.set 显式写入：实体方式会被 MP 从 SET 子句排除导致静默失效
        LambdaUpdateWrapper<ResumeShare> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ResumeShare::getUserId, userId)
                .eq(ResumeShare::getResumeId, resumeId)
                .eq(ResumeShare::getDeleted, BizConstant.NOT_DELETED)
                .set(ResumeShare::getStatus, SHARE_STATUS_REVOKED)
                .set(ResumeShare::getRevokedAt, LocalDateTime.now())
                .set(ResumeShare::getDeleted, BizConstant.DELETED)
                .set(ResumeShare::getUpdatedAt, LocalDateTime.now());
        resumeShareMapper.update(null, wrapper);
    }

    /**
     * 校验简历归属（走 ResumeService.getResumeEntity 统一访问控制，避免散落 ResumeMapper 调用）。
     */
    private void resumeServiceAccessCheck(String userId, String resumeId) {
        resumeService.getResumeEntity(userId, resumeId);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private ShareResponse toResponse(ResumeShare share) {
        ShareResponse response = new ShareResponse();
        response.setToken(share.getToken());
        response.setUrl("/share/" + share.getToken());
        response.setStatus(share.getStatus());
        response.setHideContact(Boolean.TRUE.equals(share.getHideContact()));
        response.setExpiresAt(share.getExpiresAt());
        response.setCreatedAt(share.getCreatedAt());
        return response;
    }
}
