package com.resume.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.resume.avatar.service.AvatarService;
import com.resume.common.constant.BizConstant;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.common.enums.SectionType;
import com.resume.common.service.AuditLogService;
import com.resume.common.service.RichTextSanitizer;
import com.resume.pdf.service.PdfService;
import com.resume.resume.dto.*;
import com.resume.resume.dto.AdminResumeListItemResponse;
import com.resume.resume.dto.AdminResumeStatsResponse;
import com.resume.resume.entity.Resume;
import com.resume.resume.mapper.ResumeMapper;
import com.resume.template.service.TemplateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 简历业务服务。
 */
@Slf4j
@Service
public class ResumeService {

    private final ResumeMapper resumeMapper;
    private final TemplateService templateService;
    private final PdfService pdfService;
    private final AvatarService avatarService;
    private final ResumeSectionValidator resumeSectionValidator;
    private final AuditLogService auditLogService;
    private final com.resume.resume.share.service.ShareService shareService;
    private final com.resume.audit.service.ContentAuditService contentAuditService;

    public ResumeService(ResumeMapper resumeMapper,
                          TemplateService templateService,
                          @Lazy PdfService pdfService,
                          @Lazy AvatarService avatarService,
                          ResumeSectionValidator resumeSectionValidator,
                          AuditLogService auditLogService,
                          @Lazy com.resume.resume.share.service.ShareService shareService,
                          com.resume.audit.service.ContentAuditService contentAuditService) {
        this.resumeMapper = resumeMapper;
        this.templateService = templateService;
        this.pdfService = pdfService;
        this.avatarService = avatarService;
        this.resumeSectionValidator = resumeSectionValidator;
        this.auditLogService = auditLogService;
        this.shareService = shareService;
        this.contentAuditService = contentAuditService;
    }

    private static final int MAX_TITLE_LENGTH = 128;

    /**
     * 创建简历。
     */
    @Transactional(rollbackFor = Exception.class)
    public ResumeDetailResponse createResume(String userId, CreateResumeRequest request) {
        log.info("createResume start: userId={}, scene={}, templateId={}",
                userId, request.getScene(), request.getTemplateId());
        validateScene(request.getScene());
        templateService.getTemplateEntity(request.getTemplateId());

        String title = StringUtils.isNotBlank(request.getTitle()) ? request.getTitle().trim() : generateDefaultTitle(userId);
        if (title.length() > MAX_TITLE_LENGTH) {
            title = title.substring(0, MAX_TITLE_LENGTH);
        }

        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setTitle(title);
        resume.setScene(request.getScene());
        resume.setTargetPosition(request.getTargetPosition());
        resume.setTargetIndustry(request.getTargetIndustry());
        resume.setTemplateId(request.getTemplateId());
        resume.setSections(buildDefaultSections());
        resume.setRenderSettings(RenderSettings.defaults());
        resume.setStatus(BizConstant.RESUME_STATUS_ACTIVE);
        resume.setDeleted(BizConstant.NOT_DELETED);
        resume.setExportCount(0);
        resume.setLastEditedAt(LocalDateTime.now());
        resume.setCreatedAt(LocalDateTime.now());
        resume.setUpdatedAt(LocalDateTime.now());
        resumeMapper.insert(resume);

        // 简历进入内容审核队列（待审核）
        contentAuditService.createForResume(userId, resume.getId(), title);

        log.info("createResume success: userId={}, resumeId={}", userId, resume.getId());
        return toDetailResponse(resume);
    }

    /**
     * 获取简历列表。
     */
    public Page<ResumeListItemResponse> listResumes(String userId, int page, int size, String keyword, String scene, String targetPosition) {
        // 防御：Service 层再钳制，避免内部调用绕过 Controller 校验导致深分页/超长 LIKE
        int safePage = Math.max(1, Math.min(page, 1000));
        int safeSize = Math.max(1, Math.min(size, 100));
        LambdaQueryWrapper<Resume> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Resume::getUserId, userId)
                .eq(Resume::getDeleted, BizConstant.NOT_DELETED);
        if (StringUtils.isNotBlank(scene)) {
            wrapper.eq(Resume::getScene, scene.trim().toLowerCase());
        }
        if (StringUtils.isNotBlank(targetPosition)) {
            String tp = escapeLike(targetPosition.trim());
            if (tp.length() > 64) tp = tp.substring(0, 64);
            wrapper.like(Resume::getTargetPosition, tp);
        }
        if (StringUtils.isNotBlank(keyword)) {
            String kw = escapeLike(keyword.trim());
            if (kw.length() > 64) kw = kw.substring(0, 64);
            final String fkw = kw;
            wrapper.and(w -> w.like(Resume::getTitle, fkw).or().like(Resume::getTargetPosition, fkw).or().like(Resume::getScene, fkw));
        }
        wrapper.orderByDesc(Resume::getLastEditedAt);
        Page<Resume> pageParam = new Page<>(safePage, safeSize);
        Page<Resume> result = resumeMapper.selectPage(pageParam, wrapper);

        List<ResumeListItemResponse> list = result.getRecords().stream()
                .map(this::toListItemResponse)
                .toList();
        Page<ResumeListItemResponse> responsePage = new Page<>();
        responsePage.setRecords(list);
        responsePage.setTotal(result.getTotal());
        responsePage.setCurrent(result.getCurrent());
        responsePage.setSize(result.getSize());
        responsePage.setPages(result.getPages());
        return responsePage;
    }

    public Page<ResumeListItemResponse> listResumes(String userId, int page, int size) {
        return listResumes(userId, page, size, null, null, null);
    }

    /**
     * 后台简历列表。
     */
    public Page<AdminResumeListItemResponse> listAdminResumes(int page, int size, String keyword) {
        LambdaQueryWrapper<Resume> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Resume::getDeleted, BizConstant.NOT_DELETED);
        if (StringUtils.isNotBlank(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Resume::getTitle, kw)
                    .or().like(Resume::getTargetPosition, kw));
        }
        wrapper.orderByDesc(Resume::getLastEditedAt);

        Page<Resume> pageParam = new Page<>(page, size);
        Page<Resume> result = resumeMapper.selectPage(pageParam, wrapper);

        // 批量查询模板名称，避免逐条 N+1 查询
        Map<String, String> templateNameMap = templateService.getTemplateNameMap(
                result.getRecords().stream()
                        .map(Resume::getTemplateId)
                        .filter(StringUtils::isNotBlank)
                        .collect(java.util.stream.Collectors.toSet()));

        List<AdminResumeListItemResponse> list = result.getRecords().stream()
                .map(r -> toAdminListItemResponse(r, templateNameMap))
                .toList();
        Page<AdminResumeListItemResponse> responsePage = new Page<>();
        responsePage.setRecords(list);
        responsePage.setTotal(result.getTotal());
        responsePage.setCurrent(result.getCurrent());
        responsePage.setSize(result.getSize());
        responsePage.setPages(result.getPages());
        return responsePage;
    }

    /**
     * 后台简历统计。
     */
    public AdminResumeStatsResponse adminStats() {
        AdminResumeStatsResponse response = new AdminResumeStatsResponse();

        LambdaQueryWrapper<Resume> notDeleted = new LambdaQueryWrapper<>();
        notDeleted.eq(Resume::getDeleted, BizConstant.NOT_DELETED);
        response.setTotalResumes(resumeMapper.selectCount(notDeleted));

        LambdaQueryWrapper<Resume> active = new LambdaQueryWrapper<>();
        active.eq(Resume::getDeleted, BizConstant.NOT_DELETED)
                .eq(Resume::getStatus, BizConstant.RESUME_STATUS_ACTIVE);
        response.setActiveResumes(resumeMapper.selectCount(active));

        // 已删除简历：@TableLogic 会拦截显式 deleted 条件，需用自定义 SQL 统计真实逻辑删除行
        response.setDeletedResumes(resumeMapper.countDeletedResumes());

        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        LambdaQueryWrapper<Resume> todayNew = new LambdaQueryWrapper<>();
        todayNew.eq(Resume::getDeleted, BizConstant.NOT_DELETED)
                .ge(Resume::getCreatedAt, todayStart);
        response.setTodayNewResumes(resumeMapper.selectCount(todayNew));

        return response;
    }

    /**
     * 获取简历详情。
     */
    public ResumeDetailResponse getResume(String userId, String resumeId) {
        Resume resume = getResumeEntity(userId, resumeId);
        return toDetailResponse(resume);
    }

    /**
     * 获取简历实体（供无认证预览使用，不做权限校验）。
     */
    public Resume getResumeForPreview(String resumeId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null || BizConstant.DELETED.equals(resume.getDeleted())) {
            throw new BusinessException(ResultCode.RESUME_NOT_FOUND, "简历不存在。");
        }
        return resume;
    }

    /**
     * 更新简历。
     */
    @Transactional(rollbackFor = Exception.class)
    public UpdateResumeResponse updateResume(String userId, String resumeId, UpdateResumeRequest request) {
        Resume resume = getResumeEntity(userId, resumeId);

        if (StringUtils.isNotBlank(request.getTitle())) {
            resume.setTitle(request.getTitle().trim());
        }
        if (StringUtils.isNotBlank(request.getScene())) {
            validateScene(request.getScene());
            resume.setScene(request.getScene());
        }
        if (StringUtils.isNotBlank(request.getTargetPosition())) {
            resume.setTargetPosition(request.getTargetPosition());
        }
        if (request.getTargetIndustry() != null) {
            resume.setTargetIndustry(request.getTargetIndustry());
        }
        if (StringUtils.isNotBlank(request.getTemplateId())) {
            templateService.getTemplateEntity(request.getTemplateId());
            resume.setTemplateId(request.getTemplateId());
        }
        if (request.getSections() != null) {
            if (request.getSections().isEmpty()
                    && resume.getSections() != null && !resume.getSections().isEmpty()) {
                log.error("Rejected update with empty sections to protect existing data: userId={}, resumeId={}",
                        userId, resumeId);
                throw new BusinessException(ResultCode.RESUME_SECTION_INVALID, "简历模块不能为空。");
            }
            sanitizeSections(request.getSections());
            resumeSectionValidator.validateDraft(request.getSections());
            resume.setSections(request.getSections());
        }
        if (request.getRenderSettings() != null) {
            resume.setRenderSettings(RenderSettings.sanitized(request.getRenderSettings()));
        }

        // 乐观锁 CAS：请求携带 version 时以请求值为准参与 WHERE version=? 比较
        // （OptimisticLockerInnerInterceptor 生成条件，不匹配时 updateById 返回 0 → 抛 2012 冲突）；
        // 未携带 version 时保持"读最新实体→写回"的旧行为，向后兼容旧客户端。
        Integer oldVersion = resume.getVersion();
        if (request.getVersion() != null) {
            oldVersion = request.getVersion();
            resume.setVersion(request.getVersion());
        }

        LocalDateTime now = LocalDateTime.now();
        resume.setLastEditedAt(now);
        resume.setUpdatedAt(now);
        if (resumeMapper.updateById(resume) == 0) {
            throw new BusinessException(ResultCode.RESUME_VERSION_CONFLICT,
                    "简历已被其他编辑修改，请刷新后重试。");
        }
        // 兼容 Mock（单测中 updateById 不会触发拦截器自动 +1）与真实 DB（拦截器已 +1）：
        // 若内存 version 仍等于 oldVersion，说明拦截器未生效，需手动 +1；否则已是 newVersion
        if (resume.getVersion() != null && resume.getVersion().equals(oldVersion)) {
            resume.setVersion(oldVersion == null ? 1 : oldVersion + 1);
        } else if (resume.getVersion() == null && oldVersion == null) {
            resume.setVersion(1);
        }

        UpdateResumeResponse response = new UpdateResumeResponse();
        response.setId(resume.getId());
        response.setUpdatedAt(resume.getUpdatedAt());
        response.setVersion(resume.getVersion());
        return response;
    }

    /**
     * 删除简历（逻辑删除）。
     * <p>
     * 同步清理该简历关联的 PDF 任务与头像任务，包括 MinIO 文件。
     * </p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteResume(String userId, String resumeId) {
        Resume resume = getResumeEntity(userId, resumeId);

        pdfService.cleanupTasksByResume(userId, resumeId);
        avatarService.cleanupTasksByResume(userId, resumeId);
        shareService.revokeByResume(resumeId);

        resumeMapper.deleteById(resumeId);
        auditLogService.record(userId, "delete_resume", resumeId, "title=" + resume.getTitle());
    }

    /**
     * 复制简历。
     */
    @Transactional(rollbackFor = Exception.class)
    public DuplicateResumeResponse duplicateResume(String userId, String resumeId) {
        Resume source = getResumeEntity(userId, resumeId);
        Resume copy = new Resume();
        copy.setUserId(userId);
        copy.setTitle(buildDuplicateTitle(source.getTitle()));
        copy.setScene(source.getScene());
        copy.setTargetPosition(source.getTargetPosition());
        copy.setTargetIndustry(source.getTargetIndustry());
        copy.setTemplateId(source.getTemplateId());
        // 深拷贝 sections，避免与源对象共享同一 List/Map 引用导致事务内污染
        if (source.getSections() != null) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                String json = om.writeValueAsString(source.getSections());
                java.util.List<com.resume.resume.dto.SectionDTO> copied = om.readValue(json,
                        om.getTypeFactory().constructCollectionType(java.util.List.class, com.resume.resume.dto.SectionDTO.class));
                copy.setSections(copied);
            } catch (Exception e) {
                copy.setSections(new java.util.ArrayList<>(source.getSections()));
            }
        }
        copy.setRenderSettings(RenderSettings.copyOf(source.getRenderSettings()));
        copy.setStatus(BizConstant.RESUME_STATUS_ACTIVE);
        copy.setDeleted(BizConstant.NOT_DELETED);
        copy.setExportCount(0);
        copy.setLastEditedAt(LocalDateTime.now());
        copy.setCreatedAt(LocalDateTime.now());
        copy.setUpdatedAt(LocalDateTime.now());
        resumeMapper.insert(copy);

        auditLogService.record(userId, "duplicate_resume", source.getId(), "newResumeId=" + copy.getId());

        DuplicateResumeResponse response = new DuplicateResumeResponse();
        response.setId(copy.getId());
        response.setTitle(copy.getTitle());
        response.setCreatedAt(copy.getCreatedAt());
        return response;
    }

    /**
     * 重命名简历。
     */
    @Transactional(rollbackFor = Exception.class)
    public RenameResumeResponse renameResume(String userId, String resumeId, RenameResumeRequest request) {
        Resume resume = getResumeEntity(userId, resumeId);
        Integer oldVersion = resume.getVersion();
        resume.setTitle(request.getTitle().trim());
        LocalDateTime now = LocalDateTime.now();
        resume.setLastEditedAt(now);
        resume.setUpdatedAt(now);
        if (resumeMapper.updateById(resume) == 0) {
            throw new BusinessException(ResultCode.RESUME_VERSION_CONFLICT,
                    "简历已被其他编辑修改，请刷新后重试。");
        }
        if (resume.getVersion() != null && resume.getVersion().equals(oldVersion)) {
            resume.setVersion(oldVersion == null ? 1 : oldVersion + 1);
        } else if (resume.getVersion() == null && oldVersion == null) {
            resume.setVersion(1);
        }

        RenameResumeResponse response = new RenameResumeResponse();
        response.setId(resume.getId());
        response.setTitle(resume.getTitle());
        response.setUpdatedAt(resume.getUpdatedAt());
        return response;
    }

    /**
     * 递增简历导出次数（原子更新，避免并发导出时计数丢失）。
     */
    public void incrementExportCount(String resumeId) {
        LambdaUpdateWrapper<Resume> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Resume::getId, resumeId)
                .eq(Resume::getDeleted, BizConstant.NOT_DELETED)
                .setSql("export_count = COALESCE(export_count, 0) + 1")
                .set(Resume::getUpdatedAt, LocalDateTime.now());
        resumeMapper.update(null, wrapper);
    }

    public Resume getResumeEntity(String userId, String resumeId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null || BizConstant.DELETED.equals(resume.getDeleted())) {
            throw new BusinessException(ResultCode.RESUME_NOT_FOUND, "简历不存在。");
        }
        if (!userId.equals(resume.getUserId())) {
            throw new BusinessException(ResultCode.ACCESS_DENIED, "无权访问该资源。");
        }
        return resume;
    }

    /**
     * 将一寸照地址回填到指定简历的 profile Section 的 avatarUrl 字段。
     * <p>
     * 由 {@code AvatarService.optimize} 在优化成功后调用。若简历未找到或已被删除，
     * 则跳过回填（仅记录 warn 日志），不影响头像优化任务本身的成功状态。
     * </p>
     *
     * @param userId     用户 ID
     * @param resumeId   关联简历 ID，可为 null
     * @param avatarUrl  一寸照可访问 URL
     */
    @Transactional(rollbackFor = Exception.class)
    public void fillAvatarUrl(String userId, String resumeId, String avatarUrl) {
        if (StringUtils.isBlank(userId) || StringUtils.isBlank(resumeId) || StringUtils.isBlank(avatarUrl)) {
            return;
        }
        // 防御：仅允许 http/https 或站内 /uploads/，且需通过路径穿越校验
        String trimmed = avatarUrl.trim();
        String lower = trimmed.toLowerCase();
        if (!(lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("/uploads/"))) {
            log.warn("fillAvatarUrl skipped: illegal url scheme, userId={}, resumeId={}", userId, resumeId);
            return;
        }
        if (lower.contains("..") || lower.contains("\\") || lower.contains("%2e")) {
            log.warn("fillAvatarUrl skipped: illegal url content, userId={}, resumeId={}", userId, resumeId);
            return;
        }
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null || BizConstant.DELETED.equals(resume.getDeleted())
                || !userId.equals(resume.getUserId())) {
            log.warn("fillAvatarUrl skipped: resume not found or not owned, userId={}, resumeId={}", userId, resumeId);
            return;
        }
        List<SectionDTO> sections = resume.getSections();
        if (sections == null || sections.isEmpty()) {
            return;
        }
        boolean updated = false;
        for (SectionDTO section : sections) {
            if (BizConstant.SECTION_TYPE_PROFILE.equals(section.getType())
                    && section.getData() instanceof Map) {
                section.dataAsMap().put("avatarUrl", avatarUrl);
                updated = true;
                break;
            }
        }
        if (updated) {
            Integer oldVersion = resume.getVersion();
            resume.setSections(sections);
            resume.setLastEditedAt(LocalDateTime.now());
            resume.setUpdatedAt(LocalDateTime.now());
            // 回填属于旁路写入：若用户正在编辑导致版本冲突，跳过回填而不影响头像任务成功状态
            if (resumeMapper.updateById(resume) == 0) {
                log.warn("fillAvatarUrl skipped: optimistic lock conflict, userId={}, resumeId={}", userId, resumeId);
                return;
            }
            if (resume.getVersion() != null && resume.getVersion().equals(oldVersion)) {
                resume.setVersion(oldVersion == null ? 1 : oldVersion + 1);
            } else if (resume.getVersion() == null && oldVersion == null) {
                resume.setVersion(1);
            }
            log.info("Avatar URL filled: userId={}, resumeId={}", userId, resumeId);
        }
    }

    /**
     * @deprecated 乐观锁版本已由 MyBatis-Plus 在 updateById 后自动同步到内存，无需手动 +1；保留空实现以兼容旧调用
     */
    @SuppressWarnings("unused")
    private void incrementVersion(Resume resume) {
        // no-op: 旧逻辑会导致 version 比 DB 多 1，引发后续保存必冲突（表现为第二次眼睛切换 409）
    }

    private void validateScene(String scene) {
        if (StringUtils.isBlank(scene) || !BizConstant.SCENES.contains(scene)) {
            throw new BusinessException(ResultCode.RESUME_SCENE_INVALID, "使用场景不正确。");
        }
    }

    private String buildDuplicateTitle(String sourceTitle) {
        String suffix = " 副本";
        String safe = StringUtils.defaultString(sourceTitle, "");
        String title = safe + suffix;
        if (title.length() > MAX_TITLE_LENGTH) {
            int keep = Math.max(0, MAX_TITLE_LENGTH - suffix.length());
            title = safe.substring(0, keep) + suffix;
        }
        return title;
    }

    private String generateDefaultTitle(String userId) {
        long count = resumeMapper.selectCount(
                new LambdaQueryWrapper<Resume>()
                        .eq(Resume::getUserId, userId)
                        .eq(Resume::getDeleted, BizConstant.NOT_DELETED));
        return "我的简历 " + (count + 1);
    }

    private List<SectionDTO> buildDefaultSections() {
        List<SectionDTO> sections = new ArrayList<>();
        sections.add(createSection(SectionType.PROFILE.getCode(), "个人信息", 0, new HashMap<String, Object>()));
        sections.add(createSection(SectionType.EDUCATION.getCode(), "教育经历", 1, new ArrayList<>()));
        sections.add(createSection(SectionType.PROJECT.getCode(), "项目经历", 2, new ArrayList<>()));
        sections.add(createSection(SectionType.WORK.getCode(), "工作经历", 3, new ArrayList<>()));
        sections.add(createSection(SectionType.SKILL.getCode(), "技能 & 技术栈", 4, new ArrayList<>()));
        sections.add(createSection(SectionType.INTRODUCTION.getCode(), "自我介绍", 5, new HashMap<String, Object>()));
        return sections;
    }

    private SectionDTO createSection(String type, String title, int order, Object data) {
        SectionDTO section = new SectionDTO();
        section.setId("sec_" + java.util.UUID.randomUUID().toString().replace("-", "") + "_" + order);
        section.setType(type);
        section.setTitle(title);
        section.setOrder(order);
        section.setVisible(true);
        section.setData(data);
        return section;
    }

    private ResumeDetailResponse toDetailResponse(Resume resume) {
        ResumeDetailResponse response = new ResumeDetailResponse();
        response.setId(resume.getId());
        response.setUserId(resume.getUserId());
        response.setTitle(resume.getTitle());
        response.setScene(resume.getScene());
        response.setTargetPosition(resume.getTargetPosition());
        response.setTargetIndustry(resume.getTargetIndustry());
        response.setTemplateId(resume.getTemplateId());
        response.setSections(resume.getSections());
        response.setRenderSettings(RenderSettings.copyOf(resume.getRenderSettings()));
        response.setExportCount(resume.getExportCount());
        response.setVersion(resume.getVersion());
        response.setLastEditedAt(resume.getLastEditedAt());
        response.setCreatedAt(resume.getCreatedAt());
        response.setUpdatedAt(resume.getUpdatedAt());
        return response;
    }

    private ResumeListItemResponse toListItemResponse(Resume resume) {
        ResumeListItemResponse response = new ResumeListItemResponse();
        response.setId(resume.getId());
        response.setTitle(resume.getTitle());
        response.setScene(resume.getScene());
        response.setTargetPosition(resume.getTargetPosition());
        response.setTemplateId(resume.getTemplateId());
        response.setLastEditedAt(resume.getLastEditedAt());
        response.setCreatedAt(resume.getCreatedAt());
        response.setUpdatedAt(resume.getUpdatedAt());
        return response;
    }

    private AdminResumeListItemResponse toAdminListItemResponse(Resume resume, Map<String, String> templateNameMap) {
        AdminResumeListItemResponse response = new AdminResumeListItemResponse();
        response.setId(resume.getId());
        response.setUserId(resume.getUserId());
        response.setTitle(resume.getTitle());
        response.setScene(resume.getScene());
        response.setTargetPosition(resume.getTargetPosition());
        response.setTemplateId(resume.getTemplateId());
        response.setExportCount(resume.getExportCount());
        response.setStatus(resume.getStatus());
        response.setLastEditedAt(resume.getLastEditedAt());
        response.setCreatedAt(resume.getCreatedAt());
        response.setUpdatedAt(resume.getUpdatedAt());
        response.setTemplateName(StringUtils.defaultIfBlank(
                templateNameMap.get(resume.getTemplateId()), "未知模板"));
        return response;
    }

    /**
     * 净化富文本字段，防止存储型 XSS 持久化。
     * 通用：对所有以 Html 结尾的字段做白名单清洗，新增富文本字段无需额外维护。
     * 覆盖 introduction.contentHtml、work/project descriptionHtml/achievementsHtml 及未来扩展。
     */
    String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private void sanitizeSections(List<SectionDTO> sections) {
        if (sections == null) return;
        for (SectionDTO section : sections) {
            Object data = section.getData();
            if (data instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) data;
                for (Map.Entry<String, Object> e : map.entrySet()) {
                    if (e.getKey() != null && e.getKey().endsWith("Html") && e.getValue() instanceof String html) {
                        map.put(e.getKey(), RichTextSanitizer.sanitize(html));
                    }
                }
            } else if (data instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> items = (List<Object>) data;
                for (Object obj : items) {
                    if (obj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> item = (Map<String, Object>) obj;
                        for (Map.Entry<String, Object> e : item.entrySet()) {
                            if (e.getKey() != null && e.getKey().endsWith("Html") && e.getValue() instanceof String html) {
                                item.put(e.getKey(), RichTextSanitizer.sanitize(html));
                            }
                        }
                    }
                }
            }
        }
    }
}
