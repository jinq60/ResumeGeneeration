package com.resume.resume.service;

import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.common.service.RichTextSanitizer;
import com.resume.resume.dto.RenderSettings;
import com.resume.resume.dto.SectionDTO;
import com.resume.resume.entity.Resume;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.docx4j.convert.in.xhtml.XHTMLImporter;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 简历多格式导出（Markdown / Word）。
 * <p>
 * Markdown 零依赖拼接；Word 使用"Word 友好"简化 HTML 经 docx4j 转换，
 * 保证导出为可编辑文档且中文正常。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeExportService {

    private final ResumeService resumeService;
    private final ResumeSectionValidator resumeSectionValidator;

    /**
     * 生成 Markdown 内容。
     */
    public String buildMarkdown(String userId, String resumeId) {
        Resume resume = resumeService.getResumeEntity(userId, resumeId);
        resumeSectionValidator.validateForExport(resume.getSections());

        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(profileValue(resume, "name")).append("\n\n");
        sb.append("> 目标岗位：").append(profileValue(resume, "targetPosition")).append("\n\n");
        String contact = buildContactLine(resume);
        if (StringUtils.isNotBlank(contact)) {
            sb.append("> ").append(contact).append("\n\n");
        }

        for (SectionDTO section : resume.getSections()) {
            if (!Boolean.TRUE.equals(section.getVisible())) {
                continue;
            }
            sb.append("## ").append(section.getTitle()).append("\n\n");
            appendSectionMarkdown(sb, section);
        }
        return sb.toString();
    }

    /**
     * 生成 Word 文档字节。
     */
    public byte[] buildWord(String userId, String resumeId) {
        Resume resume = resumeService.getResumeEntity(userId, resumeId);
        resumeSectionValidator.validateForExport(resume.getSections());

        String html = renderWordHtml(resume);
        try {
            WordprocessingMLPackage wordPackage = WordprocessingMLPackage.createPackage();
            XHTMLImporter importer = new XHTMLImporterImpl(wordPackage);
            List<Object> imports = importer.convert(html, null);
            wordPackage.getMainDocumentPart().getContent().addAll(imports);
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                wordPackage.save(out);
                return out.toByteArray();
            }
        } catch (Exception e) {
            log.error("Word export failed: resumeId={}", resumeId, e);
            throw new BusinessException(ResultCode.PDF_EXPORT_FAILED, "Word 文档生成失败，请稍后重试。");
        }
    }

    /**
     * 导出文件名（与 PDF 命名规则一致，仅扩展名不同）。
     */
    public String buildExportFileName(String userId, String resumeId, String extension) {
        Resume resume = resumeService.getResumeEntity(userId, resumeId);
        String name = sanitizeFileName(profileValue(resume, "name"));
        String targetPosition = sanitizeFileName(resume.getTargetPosition());
        String base;
        if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(targetPosition)) {
            base = name + "_" + targetPosition + "_简历";
        } else if (StringUtils.isNotBlank(name)) {
            base = name + "_简历";
        } else {
            base = "我的简历_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        }
        if (base.length() > 100) {
            base = base.substring(0, 97);
        }
        return base + "." + extension;
    }

    /**
     * 清理文件名中的路径分隔符与控制字符，避免拼接进下载文件名 / MinIO 对象键时产生歧义。
     */
    private String sanitizeFileName(String name) {
        if (name == null) {
            return "";
        }
        return name.replaceAll("[\\\\/:*?\"<>|\\r\\n\\t]", "_").trim();
    }

    /**
     * Word 友好 HTML：仅段落/无序列表/行内加粗，无 CSS 依赖。
     * 注意：docx4j XHTMLImporter 按严格 XML（XHTML）解析，所有空标签必须自闭合。
     */
    private String renderWordHtml(Resume resume) {
        RenderSettings settings = RenderSettings.sanitized(resume.getRenderSettings());
        String fontFamily = settings.getFontFamily() != null
                ? settings.getFontFamily() : RenderSettings.DEFAULT_FONT_FAMILY;
        String fontSize = settings.getBaseFontSize() != null
                ? cssNumber(settings.getBaseFontSize()) + "pt" : "10.5pt";
        String lineHeight = settings.getLineHeight() != null
                ? cssNumber(settings.getLineHeight()) : "1.5";
        String accentColor = settings.getAccentColor() != null
                ? settings.getAccentColor() : RenderSettings.DEFAULT_ACCENT_COLOR;
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html><head><meta charset=\"UTF-8\"/>")
                .append("<style>body { font-family: ").append(fontFamily)
                .append("; font-size: ").append(fontSize).append("; line-height: ")
                .append(lineHeight).append("; } h2 { color: ").append(accentColor)
                .append("; border-bottom: 1px solid ").append(accentColor)
                .append("; }</style></head><body>\n");
        sb.append("<h1>").append(escapeHtml(profileValue(resume, "name"))).append("</h1>\n");
        String contact = buildContactLine(resume);
        if (StringUtils.isNotBlank(contact)) {
            sb.append("<p>").append(escapeHtml(contact)).append("</p>\n");
        }
        String targetPosition = profileValue(resume, "targetPosition");
        if (StringUtils.isNotBlank(targetPosition)) {
            sb.append("<p><strong>目标岗位：</strong>").append(escapeHtml(targetPosition)).append("</p>\n");
        }
        for (SectionDTO section : resume.getSections()) {
            if (!Boolean.TRUE.equals(section.getVisible())) {
                continue;
            }
            sb.append("<h2>").append(escapeHtml(section.getTitle())).append("</h2>\n");
            appendSectionHtml(sb, section);
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    private void appendSectionMarkdown(StringBuilder sb, SectionDTO section) {
        switch (section.getType()) {
            case "profile" -> appendProfileMarkdown(sb, section.dataAsMap());
            case "introduction" -> {
                Map<String, Object> intro = section.dataAsMap();
                String richContent = getString(intro, "contentHtml");
                sb.append(StringUtils.isNotBlank(richContent)
                        ? RichTextSanitizer.toPlainText(richContent)
                        : getString(intro, "content"))
                  .append("\n\n");
            }
            case "education", "work", "project", "skill" -> {
                for (Map<String, Object> map : section.dataAsItems()) {
                    sb.append("- **").append(firstNonBlank(getString(map, "school"),
                            getString(map, "company"), getString(map, "name"), getString(map, "category")))
                      .append("**");
                    String sub = firstNonBlank(getString(map, "degree"), getString(map, "position"),
                            getString(map, "role"));
                    if (StringUtils.isNotBlank(sub)) {
                        sb.append(" · ").append(sub);
                    }
                    sb.append("\n");
                     appendRichTextMarkdown(sb, map, "descriptionHtml", "description");
                     appendRichTextMarkdown(sb, map, "achievementsHtml", "achievements");
                     appendListMarkdown(sb, map, "items");
                }
                sb.append("\n");
            }
            default -> sb.append("\n");
        }
    }

    private void appendSectionHtml(StringBuilder sb, SectionDTO section) {
        switch (section.getType()) {
            case "profile" -> { /* 已在头部输出 */ }
            case "introduction" -> {
                Map<String, Object> intro = section.dataAsMap();
                String richContent = getString(intro, "contentHtml");
                if (StringUtils.isNotBlank(richContent)) {
                    sb.append(RichTextSanitizer.sanitize(richContent)).append("\n");
                } else {
                    sb.append("<p>").append(escapeHtml(getString(intro, "content"))).append("</p>\n");
                }
            }
            case "education", "work", "project", "skill" -> {
                for (Map<String, Object> map : section.dataAsItems()) {
                    String title = firstNonBlank(getString(map, "school"), getString(map, "company"),
                            getString(map, "name"), getString(map, "category"));
                    String sub = firstNonBlank(getString(map, "degree"), getString(map, "position"),
                            getString(map, "role"));
                    sb.append("<p><strong>").append(escapeHtml(title)).append("</strong>");
                    if (StringUtils.isNotBlank(sub)) {
                        sb.append(" · ").append(escapeHtml(sub));
                    }
                    sb.append("</p>\n");
                     appendRichTextHtml(sb, map, "descriptionHtml", "description");
                     appendRichTextHtml(sb, map, "achievementsHtml", "achievements");
                     appendListHtml(sb, map, "items");
                }
            }
            default -> { }
        }
    }

    private void appendProfileMarkdown(StringBuilder sb, Map<String, Object> profile) {
        String contact = buildContactLine(profile);
        if (StringUtils.isNotBlank(contact)) {
            sb.append(contact).append("\n\n");
        }
    }

    private void appendListMarkdown(StringBuilder sb, Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof List<?> list) {
            for (Object obj : list) {
                if (obj instanceof Map<?, ?> skill) {
                    sb.append("  - ").append(getString((Map<String, Object>) skill, "name")).append("\n");
                } else if (obj != null) {
                    sb.append("  - ").append(obj.toString()).append("\n");
                }
            }
        }
    }

    private void appendRichTextMarkdown(StringBuilder sb, Map<String, Object> map,
                                        String htmlKey, String plainKey) {
        String richText = getString(map, htmlKey);
        if (StringUtils.isNotBlank(richText)) {
            String plainText = RichTextSanitizer.toPlainText(richText);
            for (String line : plainText.split("\\R")) {
                if (StringUtils.isNotBlank(line)) {
                    sb.append("  - ").append(line.trim()).append("\n");
                }
            }
            return;
        }
        appendListMarkdown(sb, map, plainKey);
    }

    private void appendListHtml(StringBuilder sb, Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof List<?> list && !list.isEmpty()) {
            sb.append("<ul>\n");
            for (Object obj : list) {
                if (obj instanceof Map<?, ?> skill) {
                    sb.append("<li>").append(escapeHtml(getString((Map<String, Object>) skill, "name"))).append("</li>\n");
                } else if (obj != null) {
                    sb.append("<li>").append(escapeHtml(obj.toString())).append("</li>\n");
                }
            }
            sb.append("</ul>\n");
        }
    }

    @SuppressWarnings("unchecked")
    private void appendRichTextHtml(StringBuilder sb, Map<String, Object> map,
                                    String htmlKey, String plainKey) {
        String richText = getString(map, htmlKey);
        if (StringUtils.isNotBlank(richText)) {
            sb.append(RichTextSanitizer.sanitize(richText)).append("\n");
            return;
        }
        appendListHtml(sb, map, plainKey);
    }

    private String buildContactLine(Resume resume) {
        return buildContactLine(findProfile(resume.getSections()));
    }

    private String buildContactLine(Map<String, Object> profile) {
        StringBuilder sb = new StringBuilder();
        String phone = getString(profile, "phone");
        String email = getString(profile, "email");
        String city = getString(profile, "city");
        if (StringUtils.isNotBlank(phone)) sb.append(phone);
        if (StringUtils.isNotBlank(email)) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append(email);
        }
        if (StringUtils.isNotBlank(city)) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append(city);
        }
        return sb.toString();
    }

    private String profileValue(Resume resume, String key) {
        return getString(findProfile(resume.getSections()), key);
    }

    private Map<String, Object> findProfile(List<SectionDTO> sections) {
        if (sections == null) {
            return Map.of();
        }
        return sections.stream()
                .filter(s -> "profile".equals(s.getType()))
                .map(SectionDTO::dataAsMap)
                .filter(m -> !m.isEmpty())
                .findFirst()
                .orElse(Map.of());
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return "";
    }

    private String cssNumber(Double value) {
        return value % 1 == 0 ? String.valueOf(value.intValue()) : String.valueOf(value);
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
