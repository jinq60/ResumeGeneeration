package com.resume.resume.share.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.resume.common.constant.BizConstant;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.common.service.ResumeRenderService;
import com.resume.resume.entity.Resume;
import com.resume.resume.mapper.ResumeMapper;
import com.resume.resume.share.dto.ShareResponse;
import com.resume.resume.share.entity.ResumeShare;
import com.resume.resume.share.mapper.ResumeShareMapper;
import com.resume.template.entity.Template;
import com.resume.template.service.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
@RequiredArgsConstructor
public class ShareService {

    public static final String SHARE_STATUS_ACTIVE = "active";
    public static final String SHARE_STATUS_REVOKED = "revoked";

    private final ResumeShareMapper resumeShareMapper;
    private final ResumeMapper resumeMapper;
    private final ResumeRenderService resumeRenderService;
    private final TemplateService templateService;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 创建或轮换分享（同一简历只保留一条有效分享）。
     */
    @Transactional(rollbackFor = Exception.class)
    public ShareResponse createShare(String userId, String resumeId) {
        resumeServiceAccessCheck(userId, resumeId);
        // 关闭旧的分享
        revokeShareRecords(userId, resumeId);

        ResumeShare share = new ResumeShare();
        share.setResumeId(resumeId);
        share.setUserId(userId);
        share.setToken(generateToken());
        share.setStatus(SHARE_STATUS_ACTIVE);
        share.setDeleted(BizConstant.NOT_DELETED);
        share.setCreatedAt(LocalDateTime.now());
        share.setUpdatedAt(LocalDateTime.now());
        resumeShareMapper.insert(share);
        log.info("Resume share created: resumeId={}, shareId={}", resumeId, share.getId());
        return toResponse(share);
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
        Template template = templateService.getTemplateEntity(resume.getTemplateId());
        String body = resumeRenderService.render(resume, template);
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
        ResumeShare update = new ResumeShare();
        update.setStatus(SHARE_STATUS_REVOKED);
        update.setRevokedAt(LocalDateTime.now());
        update.setDeleted(BizConstant.DELETED);
        update.setUpdatedAt(LocalDateTime.now());
        LambdaQueryWrapper<ResumeShare> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResumeShare::getResumeId, resumeId);
        resumeShareMapper.update(update, wrapper);
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
        ResumeShare update = new ResumeShare();
        update.setStatus(SHARE_STATUS_REVOKED);
        update.setRevokedAt(LocalDateTime.now());
        update.setDeleted(BizConstant.DELETED);
        update.setUpdatedAt(LocalDateTime.now());
        LambdaQueryWrapper<ResumeShare> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResumeShare::getUserId, userId)
                .eq(ResumeShare::getResumeId, resumeId)
                .eq(ResumeShare::getDeleted, BizConstant.NOT_DELETED);
        resumeShareMapper.update(update, wrapper);
    }

    /**
     * 校验简历归属（借用 ResumeService 的访问控制）。
     */
    private void resumeServiceAccessCheck(String userId, String resumeId) {
        // 仅做归属校验，不返回值
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null || BizConstant.DELETED.equals(resume.getDeleted())) {
            throw new BusinessException(ResultCode.RESUME_NOT_FOUND, "简历不存在。");
        }
        if (!userId.equals(resume.getUserId())) {
            throw new BusinessException(ResultCode.ACCESS_DENIED, "无权访问该资源。");
        }
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
        response.setExpiresAt(share.getExpiresAt());
        response.setCreatedAt(share.getCreatedAt());
        return response;
    }
}
