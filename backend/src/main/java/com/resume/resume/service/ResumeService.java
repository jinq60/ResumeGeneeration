package com.resume.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.resume.avatar.service.AvatarService;
import com.resume.common.constant.BizConstant;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.common.enums.SectionType;
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

    public ResumeService(ResumeMapper resumeMapper,
                          TemplateService templateService,
                          @Lazy PdfService pdfService,
                          @Lazy AvatarService avatarService,
                          ResumeSectionValidator resumeSectionValidator) {
        this.resumeMapper = resumeMapper;
        this.templateService = templateService;
        this.pdfService = pdfService;
        this.avatarService = avatarService;
        this.resumeSectionValidator = resumeSectionValidator;
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
        resume.setStatus(BizConstant.RESUME_STATUS_ACTIVE);
        resume.setDeleted(BizConstant.NOT_DELETED);
        resume.setExportCount(0);
        resume.setLastEditedAt(LocalDateTime.now());
        resume.setCreatedAt(LocalDateTime.now());
        resume.setUpdatedAt(LocalDateTime.now());
        resumeMapper.insert(resume);

        log.info("createResume success: userId={}, resumeId={}", userId, resume.getId());
        return toDetailResponse(resume);
    }

    /**
     * 获取简历列表。
     */
    public Page<ResumeListItemResponse> listResumes(String userId, int page, int size) {
        LambdaQueryWrapper<Resume> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Resume::getUserId, userId)
                .eq(Resume::getDeleted, BizConstant.NOT_DELETED)
                .orderByDesc(Resume::getLastEditedAt);
        Page<Resume> pageParam = new Page<>(page, size);
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

        List<AdminResumeListItemResponse> list = result.getRecords().stream()
                .map(this::toAdminListItemResponse)
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

        LambdaQueryWrapper<Resume> deleted = new LambdaQueryWrapper<>();
        deleted.eq(Resume::getDeleted, BizConstant.DELETED);
        response.setDeletedResumes(resumeMapper.selectCount(deleted));

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
            resumeSectionValidator.validateDraft(request.getSections());
            resume.setSections(request.getSections());
        }

        LocalDateTime now = LocalDateTime.now();
        resume.setLastEditedAt(now);
        resume.setUpdatedAt(now);
        resumeMapper.updateById(resume);

        UpdateResumeResponse response = new UpdateResumeResponse();
        response.setId(resume.getId());
        response.setUpdatedAt(resume.getUpdatedAt());
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

        resumeMapper.deleteById(resumeId);
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
        copy.setSections(source.getSections());
        copy.setStatus(BizConstant.RESUME_STATUS_ACTIVE);
        copy.setDeleted(BizConstant.NOT_DELETED);
        copy.setExportCount(0);
        copy.setLastEditedAt(LocalDateTime.now());
        copy.setCreatedAt(LocalDateTime.now());
        copy.setUpdatedAt(LocalDateTime.now());
        resumeMapper.insert(copy);

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
        resume.setTitle(request.getTitle().trim());
        LocalDateTime now = LocalDateTime.now();
        resume.setLastEditedAt(now);
        resume.setUpdatedAt(now);
        resumeMapper.updateById(resume);

        RenameResumeResponse response = new RenameResumeResponse();
        response.setId(resume.getId());
        response.setTitle(resume.getTitle());
        response.setUpdatedAt(resume.getUpdatedAt());
        return response;
    }

    /**
     * 递增简历导出次数。
     */
    public void incrementExportCount(String resumeId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume != null && BizConstant.NOT_DELETED.equals(resume.getDeleted())) {
            resume.setExportCount(resume.getExportCount() != null ? resume.getExportCount() + 1 : 1);
            resume.setUpdatedAt(LocalDateTime.now());
            resumeMapper.updateById(resume);
        }
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
                @SuppressWarnings("unchecked")
                Map<String, Object> profile = (Map<String, Object>) section.getData();
                profile.put("avatarUrl", avatarUrl);
                updated = true;
                break;
            }
        }
        if (updated) {
            resume.setSections(sections);
            resume.setLastEditedAt(LocalDateTime.now());
            resume.setUpdatedAt(LocalDateTime.now());
            resumeMapper.updateById(resume);
            log.info("Avatar URL filled: userId={}, resumeId={}", userId, resumeId);
        }
    }

    private void validateScene(String scene) {
        if (StringUtils.isBlank(scene) || !Arrays.asList(BizConstant.SCENES).contains(scene)) {
            throw new BusinessException(ResultCode.RESUME_SCENE_INVALID, "使用场景不正确。");
        }
    }

    private String buildDuplicateTitle(String sourceTitle) {
        String suffix = " 副本";
        String title = StringUtils.defaultString(sourceTitle, "") + suffix;
        if (title.length() > MAX_TITLE_LENGTH) {
            int keep = Math.max(0, MAX_TITLE_LENGTH - suffix.length());
            title = sourceTitle.substring(0, keep) + suffix;
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
        section.setId("sec_" + System.currentTimeMillis() + "_" + order);
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
        response.setExportCount(resume.getExportCount());
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

    private AdminResumeListItemResponse toAdminListItemResponse(Resume resume) {
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
        try {
            response.setTemplateName(templateService.getTemplate(resume.getTemplateId()).getName());
        } catch (Exception e) {
            response.setTemplateName("未知模板");
        }
        return response;
    }
}
