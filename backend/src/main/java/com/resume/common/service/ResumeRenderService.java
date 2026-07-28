package com.resume.common.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.common.constant.BizConstant;
import com.resume.resume.dto.SectionDTO;
import com.resume.resume.entity.Resume;
import com.resume.template.entity.Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 简历 HTML 渲染服务。
 * <p>
 * 统一渲染简历为 HTML，供前端预览（iframe）与 PDF 导出共用，确保预览与导出视觉一致。
 * </p>
 * <p>
 * 渲染策略：
 * <ol>
 *   <li>若 {@code template.htmlTemplate} 指向 classpath:templates/resume/{htmlTemplate}.html 文件存在，
 *       则加载该 HTML 文件并替换 {@code ${css}} / {@code ${body}} 占位符；</li>
 *   <li>否则回退到内置单栏模板，保证旧有 5 套内置模板的渲染结果不破坏；</li>
 *   <li>CSS 由 {@code template.config} 动态生成，颜色/字体/布局均来自模板配置。</li>
 * </ol>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeRenderService {

    private static final String TEMPLATE_DIR = "templates/resume/";
    private static final String TEMPLATE_SUFFIX = ".html";
    private static final String PLACEHOLDER_CSS = "${css}";
    private static final String PLACEHOLDER_BODY = "${body}";
    private static final String PLACEHOLDER_RESUME_TITLE = "${resumeTitle}";

    private final ObjectMapper objectMapper;

    /**
     * 将简历与模板渲染为完整 HTML 页面。
     *
     * @param resume   简历实体
     * @param template 模板实体
     * @return HTML 字符串
     */
    public String render(Resume resume, Template template) {
        List<SectionDTO> sections = resume.getSections() != null ? resume.getSections() : List.of();
        Map<String, Object> config = parseConfig(template.getConfig());
        String css = buildCss(config);
        String body = renderBody(sections);

        // 优先加载 template.htmlTemplate 指定的 HTML 骨架文件，支持后台新增模板。
        String skeleton = loadTemplateSkeleton(template.getHtmlTemplate());
        if (skeleton != null) {
            String resumeTitle = StringUtils.defaultString(resume.getTitle(), "");
            return skeleton
                    .replace(PLACEHOLDER_RESUME_TITLE, escapeHtml(resumeTitle))
                    .replace(PLACEHOLDER_CSS, css)
                    .replace(PLACEHOLDER_BODY, body);
        }

        // 回退：内置单栏默认模板，保证旧有内置模板仍可渲染。
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n")
            .append("<html lang=\"zh-CN\">\n")
            .append("<head>\n")
            .append("  <meta charset=\"UTF-8\">\n")
            .append("  <style>\n")
            .append(css)
            .append("  </style>\n")
            .append("</head>\n")
            .append("<body>\n")
            .append("  <div class=\"resume-page\">\n")
            .append(body)
            .append("  </div>\n")
            .append("</body>\n")
            .append("</html>");
        return html.toString();
    }

    /**
     * 渲染简历所有可见 Section，返回可嵌入骨架的 HTML 片段。
     */
    private String renderBody(List<SectionDTO> sections) {
        StringBuilder body = new StringBuilder();
        for (SectionDTO section : sections) {
            if (Boolean.TRUE.equals(section.getVisible())) {
                body.append(renderSection(section));
            }
        }
        return body.toString();
    }

    /**
     * 加载 classpath:templates/resume/{htmlTemplate}.html 骨架文件。
     * <p>
     * 找不到或读取失败时返回 null，触发回退到内置渲染逻辑。
     * </p>
     */
    private String loadTemplateSkeleton(String htmlTemplate) {
        if (StringUtils.isBlank(htmlTemplate)) {
            return null;
        }
        // 防御：仅取文件名片段，避免路径穿越
        String fileName = htmlTemplate.replaceAll("[^A-Za-z0-9._-]", "");
        if (StringUtils.isBlank(fileName)) {
            return null;
        }
        String path = TEMPLATE_DIR + fileName + TEMPLATE_SUFFIX;
        ClassPathResource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            log.debug("Template skeleton not found, fallback to builtin: {}", path);
            return null;
        }
        try (InputStream in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Read template skeleton failed: {}", path, e);
            return null;
        }
    }

    private String buildCss(Map<String, Object> config) {
        Map<String, Object> page = getMap(config, "page");
        Map<String, Object> font = getMap(config, "font");
        Map<String, Object> color = getMap(config, "color");

        StringBuilder css = new StringBuilder();
        css.append("    * { box-sizing: border-box; margin: 0; padding: 0; }\n")
           .append("    body { font-family: ").append(getString(font, "family", "\"Noto Sans SC\", \"Microsoft YaHei\", sans-serif")).append("; }\n")
           .append("    .resume-page { width: ").append(getString(page, "width", "210mm")).append(";\n")
           .append("      min-height: ").append(getString(page, "height", "297mm")).append(";\n")
           .append("      padding: ").append(getString(page, "margin", "20mm")).append(";\n")
           .append("      margin: 0 auto;\n")
           .append("      background: ").append(getString(color, "background", "#ffffff")).append(";\n")
           .append("      color: ").append(getString(color, "primary", "#333333")).append(";\n")
           .append("      font-size: ").append(getString(font, "mainSize", "10.5pt")).append("; }\n")
           .append("    .section { margin-bottom: 16px; }\n")
           .append("    .section-title { font-size: 14pt; font-weight: bold;\n")
           .append("      color: ").append(getString(color, "accent", "#1a5276")).append(";\n")
           .append("      border-bottom: 1px solid ").append(getString(color, "accent", "#1a5276")).append(";\n")
           .append("      padding-bottom: 4px; margin-bottom: 8px; }\n")
           .append("    .profile-header { display: flex; align-items: center; gap: 16px; margin-bottom: 12px; }\n")
           .append("    .profile-avatar { width: 25mm; height: 25mm; object-fit: cover; border-radius: 4px; flex-shrink: 0; }\n")
           .append("    .profile-info { flex: 1; }\n")
           .append("    .profile-name { font-size: 18pt; font-weight: bold; margin-bottom: 8px; }\n")
           .append("    .profile-meta { color: ").append(getString(color, "secondary", "#666666")).append("; margin-bottom: 4px; }\n")
           .append("    .item { margin-bottom: 12px; }\n")
           .append("    .item-header { display: flex; justify-content: space-between; font-weight: bold; }\n")
           .append("    .item-sub { color: ").append(getString(color, "secondary", "#666666")).append("; margin-bottom: 4px; }\n")
           .append("    ul { padding-left: 20px; }\n")
           .append("    li { margin-bottom: 2px; }\n")
           .append("    .skill-tag { display: inline-block; margin-right: 8px; margin-bottom: 4px;\n")
           .append("      padding: 2px 8px; background: #f0f0f0; border-radius: 4px; }\n");
        return css.toString();
    }

    private String renderSection(SectionDTO section) {
        StringBuilder sb = new StringBuilder();
        sb.append("    <div class=\"section\">\n")
          .append("      <div class=\"section-title\">").append(escapeHtml(section.getTitle())).append("</div>\n");

        Object data = section.getData();
        switch (section.getType()) {
            case BizConstant.SECTION_TYPE_PROFILE -> sb.append(renderProfile(data));
            case BizConstant.SECTION_TYPE_EDUCATION -> sb.append(renderEducation(data));
            case BizConstant.SECTION_TYPE_WORK -> sb.append(renderWork(data));
            case BizConstant.SECTION_TYPE_PROJECT -> sb.append(renderProject(data));
            case BizConstant.SECTION_TYPE_SKILL -> sb.append(renderSkill(data));
            case BizConstant.SECTION_TYPE_INTRODUCTION -> sb.append(renderIntroduction(data));
            default -> sb.append("");
        }

        sb.append("    </div>\n");
        return sb.toString();
    }

    private String renderProfile(Object data) {
        Map<String, Object> profile = toMap(data);
        boolean showAvatar = getBoolean(profile, "showAvatar", true);
        String avatarUrl = getString(profile, "avatarUrl", "");
        String name = getString(profile, "name", "");
        String phone = getString(profile, "phone", "");
        String email = getString(profile, "email", "");
        String city = getString(profile, "city", "");
        String targetPosition = getString(profile, "targetPosition", "");
        boolean showGender = getBoolean(profile, "showGender", false);
        String gender = getString(profile, "gender", "");
        boolean showAge = getBoolean(profile, "showAge", false);
        String age = getString(profile, "age", "");
        boolean showSalary = getBoolean(profile, "showSalary", false);
        String expectedSalary = getString(profile, "expectedSalary", "");
        String availability = getString(profile, "availability", "");
        String personalWebsite = getString(profile, "personalWebsite", "");
        String github = getString(profile, "github", "");
        String portfolio = getString(profile, "portfolio", "");

        StringBuilder sb = new StringBuilder();
        sb.append("      <div class=\"profile-header\">\n");
        if (showAvatar && StringUtils.isNotBlank(avatarUrl)) {
            sb.append("        <img class=\"profile-avatar\" src=\"").append(escapeHtml(avatarUrl)).append("\" alt=\"头像\">\n");
        }
        sb.append("        <div class=\"profile-info\">\n")
          .append("          <div class=\"profile-name\">").append(escapeHtml(name)).append("</div>\n");
        if (StringUtils.isNotBlank(targetPosition)) {
            sb.append("          <div class=\"profile-meta\">").append(escapeHtml(targetPosition)).append("</div>\n");
        }
        StringBuilder meta = new StringBuilder();
        if (StringUtils.isNotBlank(phone)) meta.append(phone);
        if (StringUtils.isNotBlank(email)) {
            if (meta.length() > 0) meta.append(" | ");
            meta.append(email);
        }
        if (StringUtils.isNotBlank(city)) {
            if (meta.length() > 0) meta.append(" | ");
            meta.append(city);
        }
        if (showGender && StringUtils.isNotBlank(gender)) {
            if (meta.length() > 0) meta.append(" | ");
            meta.append(gender);
        }
        if (showAge && StringUtils.isNotBlank(age)) {
            if (meta.length() > 0) meta.append(" | ");
            meta.append(age).append("岁");
        }
        if (showSalary && StringUtils.isNotBlank(expectedSalary)) {
            if (meta.length() > 0) meta.append(" | ");
            meta.append(expectedSalary);
        }
        if (StringUtils.isNotBlank(availability)) {
            if (meta.length() > 0) meta.append(" | ");
            meta.append(availability);
        }
        if (meta.length() > 0) {
            sb.append("          <div class=\"profile-meta\">").append(escapeHtml(meta.toString())).append("</div>\n");
        }
        StringBuilder links = new StringBuilder();
        if (StringUtils.isNotBlank(personalWebsite)) {
            links.append("<a href=\"").append(escapeHtml(personalWebsite)).append("\">个人网站</a>");
        }
        if (StringUtils.isNotBlank(github)) {
            if (links.length() > 0) links.append(" | ");
            links.append("<a href=\"").append(escapeHtml(github)).append("\">GitHub</a>");
        }
        if (StringUtils.isNotBlank(portfolio)) {
            if (links.length() > 0) links.append(" | ");
            links.append("<a href=\"").append(escapeHtml(portfolio)).append("\">作品集</a>");
        }
        if (links.length() > 0) {
            sb.append("          <div class=\"profile-meta\">").append(links).append("</div>\n");
        }
        sb.append("        </div>\n")
          .append("      </div>\n");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String renderEducation(Object data) {
        List<Map<String, Object>> items = (List<Map<String, Object>>) data;
        StringBuilder sb = new StringBuilder();
        if (items != null) {
            for (Map<String, Object> item : items) {
                sb.append("      <div class=\"item\">\n")
                  .append("        <div class=\"item-header\">\n")
                  .append("          <span>").append(escapeHtml(getString(item, "school", ""))).append("</span>\n")
                  .append("          <span>").append(escapeHtml(getString(item, "startDate", ""))).append(" - ")
                  .append(escapeHtml(getString(item, "endDate", ""))).append("</span>\n")
                  .append("        </div>\n")
                  .append("        <div class=\"item-sub\">")
                  .append(escapeHtml(getString(item, "degree", ""))).append(" · ")
                  .append(escapeHtml(getString(item, "major", ""))).append("</div>\n")
                  .append("      </div>\n");
            }
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String renderWork(Object data) {
        List<Map<String, Object>> items = (List<Map<String, Object>>) data;
        StringBuilder sb = new StringBuilder();
        if (items != null) {
            for (Map<String, Object> item : items) {
                sb.append("      <div class=\"item\">\n")
                  .append("        <div class=\"item-header\">\n")
                  .append("          <span>").append(escapeHtml(getString(item, "company", ""))).append(" · ")
                  .append(escapeHtml(getString(item, "position", ""))).append("</span>\n")
                  .append("          <span>").append(escapeHtml(getString(item, "startDate", ""))).append(" - ")
                  .append(escapeHtml(getString(item, "endDate", ""))).append("</span>\n")
                  .append("        </div>\n")
                  .append(renderDescriptionList((List<String>) item.get("description")))
                  .append("      </div>\n");
            }
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String renderProject(Object data) {
        List<Map<String, Object>> items = (List<Map<String, Object>>) data;
        StringBuilder sb = new StringBuilder();
        if (items != null) {
            for (Map<String, Object> item : items) {
                sb.append("      <div class=\"item\">\n")
                  .append("        <div class=\"item-header\">\n")
                  .append("          <span>").append(escapeHtml(getString(item, "name", ""))).append("</span>\n")
                  .append("          <span>").append(escapeHtml(getString(item, "startDate", ""))).append(" - ")
                  .append(escapeHtml(getString(item, "endDate", ""))).append("</span>\n")
                  .append("        </div>\n")
                  .append("        <div class=\"item-sub\">").append(escapeHtml(getString(item, "role", ""))).append("</div>\n")
                  .append(renderDescriptionList((List<String>) item.get("description")))
                  .append("      </div>\n");
            }
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String renderSkill(Object data) {
        List<Map<String, Object>> items = (List<Map<String, Object>>) data;
        StringBuilder sb = new StringBuilder();
        if (items != null) {
            for (Map<String, Object> item : items) {
                sb.append("      <div class=\"item\">\n")
                  .append("        <div class=\"item-header\">").append(escapeHtml(getString(item, "category", ""))).append("</div>\n")
                  .append("        <div>");
                List<Map<String, Object>> skills = (List<Map<String, Object>>) item.get("items");
                if (skills != null) {
                    for (Map<String, Object> skill : skills) {
                        sb.append("<span class=\"skill-tag\">").append(escapeHtml(getString(skill, "name", ""))).append("</span>");
                    }
                }
                sb.append("</div>\n")
                  .append("      </div>\n");
            }
        }
        return sb.toString();
    }

    private String renderIntroduction(Object data) {
        Map<String, Object> intro = toMap(data);
        return "      <p>" + escapeHtml(getString(intro, "content", "")) + "</p>\n";
    }

    private String renderDescriptionList(List<String> descriptions) {
        if (descriptions == null || descriptions.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("        <ul>\n");
        for (String desc : descriptions) {
            sb.append("          <li>").append(escapeHtml(desc)).append("</li>\n");
        }
        sb.append("        </ul>\n");
        return sb.toString();
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseConfig(Object config) {
        if (config == null) {
            return Map.of();
        }
        if (config instanceof Map) {
            return (Map<String, Object>) config;
        }
        if (config instanceof String json) {
            try {
                return objectMapper.readValue(json, Map.class);
            } catch (Exception e) {
                log.warn("Parse template config failed", e);
                return Map.of();
            }
        }
        try {
            return objectMapper.convertValue(config, Map.class);
        } catch (Exception e) {
            log.warn("Convert template config failed", e);
            return Map.of();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object data) {
        if (data instanceof Map) {
            return (Map<String, Object>) data;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return Map.of();
    }

    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value instanceof Boolean b) {
            return b;
        }
        return defaultValue;
    }
}
