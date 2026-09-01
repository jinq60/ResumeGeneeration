package com.resume.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.common.constant.BizConstant;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.common.service.RichTextSanitizer;
import com.resume.resume.dto.RenderSettings;
import com.resume.resume.dto.ResumeDetailResponse;
import com.resume.resume.dto.ResumeImportRequest;
import com.resume.resume.dto.SectionDTO;
import com.resume.resume.entity.Resume;
import com.resume.resume.mapper.ResumeMapper;
import com.resume.template.service.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 简历导入服务：支持 JSON 与 Markdown 两种格式，导入后直接创建简历。
 * <p>
 * JSON 格式为 {@code List<SectionDTO>} 或 {@code {"sections": [...]}}；
 * Markdown 按模块标题（##）分块解析，未知标题映射为自定义模块。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeImportService {

    private final ResumeMapper resumeMapper;
    private final TemplateService templateService;
    private final ResumeSectionValidator resumeSectionValidator;
    private final ObjectMapper objectMapper;
    private final com.resume.audit.service.ContentAuditService contentAuditService;

    private static final int MAX_TITLE_LENGTH = 128;

    /**
     * 解析导入内容并创建简历。
     */
    @Transactional(rollbackFor = Exception.class)
    public ResumeDetailResponse importResume(String userId, ResumeImportRequest request) {
        templateService.getTemplateEntity(request.getTemplateId());

        List<SectionDTO> sections = parseSections(request.getFormat(), request.getContent());
        if (sections.isEmpty()) {
            throw new BusinessException(ResultCode.RESUME_IMPORT_INVALID, "未能从导入内容中解析出任何模块。");
        }
        sanitizeSections(sections);
        resumeSectionValidator.validateDraft(sections);

        String title = StringUtils.isNotBlank(request.getTitle())
                ? request.getTitle().trim()
                : deriveTitle(sections, userId);
        if (title.length() > MAX_TITLE_LENGTH) {
            title = title.substring(0, MAX_TITLE_LENGTH);
        }

        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setTitle(title);
        resume.setScene(request.getScene());
        resume.setTargetPosition(request.getTargetPosition());
        resume.setTemplateId(request.getTemplateId());
        resume.setSections(sections);
        resume.setRenderSettings(RenderSettings.defaults());
        resume.setStatus(BizConstant.RESUME_STATUS_ACTIVE);
        resume.setDeleted(BizConstant.NOT_DELETED);
        resume.setExportCount(0);
        resume.setLastEditedAt(LocalDateTime.now());
        resume.setCreatedAt(LocalDateTime.now());
        resume.setUpdatedAt(LocalDateTime.now());
        resumeMapper.insert(resume);

        // 导入的简历同样进入内容审核队列
        contentAuditService.createForResume(userId, resume.getId(), title);

        log.info("Resume imported: userId={}, resumeId={}, format={}, sections={}",
                userId, resume.getId(), request.getFormat(), sections.size());
        return toDetailResponse(resume);
    }

    /**
     * 解析导入内容为 Section 列表。
     */
    public List<SectionDTO> parseSections(String format, String content) {
        return "markdown".equals(format)
                ? ResumeMarkdownParser.parse(content)
                : parseJson(content);
    }

    private static final int MAX_JSON_DEPTH = 20;
    private List<SectionDTO> parseJson(String content) {
        try {
            if (content.length() > 200_000) {
                throw new BusinessException(ResultCode.RESUME_IMPORT_INVALID, "导入内容过大。");
            }
            Object parsed = objectMapper.readValue(content, Object.class);
            if (exceedsDepth(parsed, 0)) {
                throw new BusinessException(ResultCode.RESUME_IMPORT_INVALID, "JSON 嵌套过深。");
            }
            if (parsed instanceof List<?> list) {
                if (list.size() > 50) throw new BusinessException(ResultCode.RESUME_IMPORT_INVALID, "模块数量超出限制。");
                return objectMapper.convertValue(list, new TypeReference<List<SectionDTO>>() {
                });
            }
            if (parsed instanceof Map<?, ?> map && map.get("sections") instanceof List<?> sections) {
                if (sections.size() > 50) throw new BusinessException(ResultCode.RESUME_IMPORT_INVALID, "模块数量超出限制。");
                return objectMapper.convertValue(sections, new TypeReference<List<SectionDTO>>() {
                });
            }
            throw new BusinessException(ResultCode.RESUME_IMPORT_INVALID, "JSON 需为模块数组或包含 sections 字段。");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Parse imported JSON failed: {}", e.getMessage());
            throw new BusinessException(ResultCode.RESUME_IMPORT_INVALID, "JSON 解析失败，请检查格式。");
        }
    }

    private boolean exceedsDepth(Object node, int depth) {
        if (depth > MAX_JSON_DEPTH) return true;
        if (node instanceof Map<?, ?> map) {
            for (Object v : map.values()) {
                if (v instanceof Map || v instanceof List) {
                    if (exceedsDepth(v, depth + 1)) return true;
                }
            }
        } else if (node instanceof List<?> list) {
            for (Object v : list) {
                if (v instanceof Map || v instanceof List) {
                    if (exceedsDepth(v, depth + 1)) return true;
                }
            }
        }
        return false;
    }

    /**
     * 未提供标题时：取首个非空模块标题，否则使用默认标题。
     */
    private String deriveTitle(List<SectionDTO> sections, String userId) {
        return sections.stream()
                .map(SectionDTO::getTitle)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .map(title -> title.length() > MAX_TITLE_LENGTH ? title.substring(0, MAX_TITLE_LENGTH) : title)
                .orElseGet(() -> generateDefaultTitle(userId));
    }

    private String generateDefaultTitle(String userId) {
        LambdaQueryWrapper<Resume> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Resume::getUserId, userId)
                .eq(Resume::getDeleted, BizConstant.NOT_DELETED);
        long count = resumeMapper.selectCount(wrapper);
        return "我的简历 " + (count + 1);
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
