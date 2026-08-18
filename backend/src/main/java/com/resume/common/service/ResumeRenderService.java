package com.resume.common.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.common.constant.BizConstant;
import com.resume.resume.dto.RenderSettings;
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
     * 渲染选项。
     */
    public record RenderOptions(boolean hideContact) {

        public static RenderOptions withHiddenContact() {
            return new RenderOptions(true);
        }
    }

    /**
     * 站内资源（/uploads/**）在 PDF/独立页面渲染时的外部可访问前缀。
     * 配置后（如 http://localhost），相对路径会被拼成绝对 URL，否则保持原样。
     */
    @org.springframework.beans.factory.annotation.Value("${app.render.public-base-url:}")
    private String publicBaseUrl;

    /**
     * 将站内相对路径拼上外部前缀，供 Playwright 以 file:// 打开 HTML 时仍能加载图片。
     */
    private String absolutizeUrl(String url) {
        if (StringUtils.isBlank(url) || StringUtils.isBlank(publicBaseUrl)) {
            return url;
        }
        String trimmed = url.trim();
        if (trimmed.startsWith("/uploads/")) {
            return publicBaseUrl + trimmed;
        }
        return url;
    }

    /**
     * 返回站外资源前缀的 origin（scheme://host[:port]），未配置或格式非法返回 null。
     * 供预览/分享页 CSP 的 img-src 放行站外头像/图片，避免与 publicBaseUrl 冲突。
     */
    public String publicBaseOrigin() {
        if (StringUtils.isBlank(publicBaseUrl)) {
            return null;
        }
        try {
            java.net.URI uri = java.net.URI.create(publicBaseUrl.trim());
            if (uri.getScheme() == null || uri.getHost() == null) {
                return null;
            }
            int port = uri.getPort();
            return uri.getScheme() + "://" + uri.getHost() + (port > 0 ? ":" + port : "");
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 将简历与模板渲染为完整 HTML 页面（默认渲染选项）。
     *
     * @param resume   简历实体
     * @param template 模板实体
     * @return HTML 字符串
     */
    public String render(Resume resume, Template template) {
        return render(resume, template, new RenderOptions(false));
    }

    /**
     * 将简历与模板渲染为完整 HTML 页面。
     *
     * @param resume   简历实体
     * @param template 模板实体
     * @param options  渲染选项（如分享页隐藏联系方式）
     * @return HTML 字符串
     */
    public String render(Resume resume, Template template, RenderOptions options) {
        List<SectionDTO> sections = resume.getSections() != null ? resume.getSections() : List.of();
        Map<String, Object> config = parseConfig(template.getConfig());
        RenderSettings settings = RenderSettings.sanitized(resume.getRenderSettings());
        String css = buildCss(config, settings);
        String body = renderBody(sections, options);

        // 优先加载 template.htmlTemplate 指定的 HTML 骨架文件，支持后台新增模板。
        String skeleton = loadTemplateSkeleton(template.getHtmlTemplate());
        if (skeleton != null) {
            String resumeTitle = StringUtils.defaultString(resume.getTitle(), "");
            String rendered = skeleton
                    .replace(PLACEHOLDER_RESUME_TITLE, escapeHtml(resumeTitle))
                    .replace(PLACEHOLDER_CSS, css)
                    .replace(PLACEHOLDER_BODY, body);
            return decoratePage(rendered, settings);
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
            .append("  <div class=\"resume-page\" data-auto-one-page=\"")
            .append(Boolean.TRUE.equals(settings.getAutoOnePage()))
            .append("\">\n")
            .append(body)
            .append("  </div>\n")
            .append("</body>\n")
            .append("</html>");
        return html.toString();
    }

    /**
     * 渲染简历所有可见 Section，返回可嵌入骨架的 HTML 片段。
     */
    private String renderBody(List<SectionDTO> sections, RenderOptions options) {
        StringBuilder body = new StringBuilder();
        for (SectionDTO section : sections) {
            if (Boolean.TRUE.equals(section.getVisible())) {
                body.append(renderSection(section, options));
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

    private String buildCss(Map<String, Object> config, RenderSettings settings) {
        Map<String, Object> page = getMap(config, "page");
        Map<String, Object> font = getMap(config, "font");
        Map<String, Object> color = getMap(config, "color");

        String pageWidth = getString(page, "width", "210mm");
        String pageHeight = getString(page, "height", "297mm");
        String pageMargin = settings.getPagePadding() != null
                ? cssNumber(settings.getPagePadding()) + "mm"
                : getString(page, "margin", "20mm");
        String fontFamily = settings.getFontFamily() != null
                ? settings.getFontFamily()
                : getString(font, "family", RenderSettings.DEFAULT_FONT_FAMILY);
        String mainFontSize = settings.getBaseFontSize() != null
                ? cssNumber(settings.getBaseFontSize()) + "pt"
                : getString(font, "mainSize", "10.5pt");
        String lineHeight = settings.getLineHeight() != null
                ? cssNumber(settings.getLineHeight())
                : getString(font, "lineHeight", "1.5");
        String accentColor = settings.getAccentColor() != null
                ? settings.getAccentColor()
                : getString(color, "accent", "#1a5276");
        String sectionSpacing = settings.getSectionSpacing() != null
                ? cssNumber(settings.getSectionSpacing()) + "px"
                : getString(config, "moduleSpacing", "16px");

        StringBuilder css = new StringBuilder();
        css.append("    * { box-sizing: border-box; margin: 0; padding: 0; }\n")
           .append("    @page { size: A4; margin: 0; }\n")
           .append("    html, body { margin: 0; padding: 0; }\n")
           .append("    body { font-family: ").append(fontFamily).append("; line-height: ").append(lineHeight).append("; }\n")
           .append("    .resume-page { width: ").append(pageWidth).append(";\n")
           .append("      min-height: ").append(pageHeight).append(";\n")
           .append("      padding: ").append(pageMargin).append(";\n")
           .append("      margin: 0 auto;\n")
           .append("      background: ").append(getString(color, "background", "#ffffff")).append(";\n")
           .append("      color: ").append(getString(color, "primary", "#333333")).append(";\n")
           .append("      font-size: ").append(mainFontSize).append("; }\n")
           .append("    .resume-page[data-auto-one-page=\"true\"] {\n")
           .append("      --resume-fit-scale: 1;\n")
           .append("      width: calc(").append(pageWidth).append(" / var(--resume-fit-scale));\n")
           .append("      min-height: calc(").append(pageHeight).append(" / var(--resume-fit-scale));\n")
           .append("      transform: scale(var(--resume-fit-scale));\n")
           .append("      transform-origin: top left;\n")
           .append("    }\n")
           .append("    .section { margin-bottom: ").append(sectionSpacing).append("; }\n")
           .append("    .section-title { font-size: 14pt; font-weight: bold;\n")
           .append("      color: ").append(accentColor).append(";\n")
           .append("      border-bottom: 1px solid ").append(accentColor).append(";\n")
           .append("      padding-bottom: 4px; margin-bottom: 8px; }\n")
           .append("    .profile-header { display: flex; align-items: center; gap: 16px; margin-bottom: 12px; }\n")
           .append("    .profile-avatar { width: 25mm; height: 25mm; object-fit: cover; border-radius: 4px; flex-shrink: 0; }\n")
           .append("    .profile-info { flex: 1; }\n")
           .append("    .profile-name { font-size: 18pt; font-weight: bold; margin-bottom: 8px; }\n")
           .append("    .profile-meta { color: ").append(getString(color, "secondary", "#666666")).append("; margin-bottom: 4px; }\n")
           .append("    .item { margin-bottom: 12px; }\n")
           .append("    .item-header { display: flex; justify-content: space-between; font-weight: bold; }\n")
           .append("    .item-sub { color: ").append(getString(color, "secondary", "#666666")).append("; margin-bottom: 4px; }\n")
           .append("    .rich-text p { margin: 0 0 6px; }\n")
           .append("    .rich-text ul, .rich-text ol { padding-left: 20px; margin: 4px 0; }\n")
           .append("    .rich-text a { color: ").append(accentColor).append("; }\n")
           .append("    ul { padding-left: 20px; }\n")
           .append("    li { margin-bottom: 2px; }\n")
           .append("    .skill-tag { display: inline-block; margin-right: 8px; margin-bottom: 4px;\n")
           .append("      padding: 2px 8px; background: #f0f0f0; border-radius: 4px; }\n");
        return css.toString();
    }

    /**
     * 对模板骨架中可能存在的硬编码样式追加用户设置，并标记一页适配开关。
     * <p>
     * 使用 Jsoup 解析 DOM 后按选择器设置属性、追加样式，避免依赖骨架中
     * {@code <div class="resume-page">} / {@code </head>} 字面量的字符串替换。
     * </p>
     */
    private String decoratePage(String html, RenderSettings settings) {
        org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(html);
        org.jsoup.nodes.Element page = doc.selectFirst("div.resume-page");
        if (page != null) {
            page.attr("data-auto-one-page", String.valueOf(Boolean.TRUE.equals(settings.getAutoOnePage())));
        }
        String override = buildOverrideCss(settings);
        if (StringUtils.isNotBlank(override)) {
            org.jsoup.nodes.Element head = doc.head();
            if (head != null) {
                head.append("<style>\n" + override + "</style>\n");
            }
        }
        doc.outputSettings().prettyPrint(false);
        return doc.html();
    }

    private String buildOverrideCss(RenderSettings settings) {
        StringBuilder override = new StringBuilder();
        if (settings.getFontFamily() != null) {
            override.append("body, .resume-page { font-family: ")
                    .append(settings.getFontFamily()).append(" !important; }\n");
        }
        if (settings.getBaseFontSize() != null) {
            override.append(".resume-page { font-size: ")
                    .append(cssNumber(settings.getBaseFontSize())).append("pt !important; }\n");
        }
        if (settings.getLineHeight() != null) {
            override.append("body, .resume-page { line-height: ")
                    .append(cssNumber(settings.getLineHeight())).append(" !important; }\n");
        }
        if (settings.getPagePadding() != null) {
            override.append(".resume-page { padding: ")
                    .append(cssNumber(settings.getPagePadding())).append("mm !important; }\n");
        }
        if (settings.getSectionSpacing() != null) {
            override.append(".section { margin-bottom: ")
                    .append(cssNumber(settings.getSectionSpacing())).append("px !important; }\n");
        }
        if (settings.getAccentColor() != null) {
            override.append(".section-title, .rich-text a { color: ")
                    .append(settings.getAccentColor()).append(" !important; }\n")
                    .append(".section-title { border-color: ")
                    .append(settings.getAccentColor()).append(" !important; }\n")
                    .append(".resume-page { --resume-accent-color: ")
                    .append(settings.getAccentColor()).append("; }\n");
        }
        return override.toString();
    }

    private String cssNumber(Double value) {
        if (value == null) {
            return "0";
        }
        return value % 1 == 0 ? String.valueOf(value.intValue()) : String.valueOf(value);
    }

    private String renderSection(SectionDTO section, RenderOptions options) {
        StringBuilder sb = new StringBuilder();
        sb.append("    <div class=\"section\">\n")
          .append("      <div class=\"section-title\">").append(escapeHtml(section.getTitle())).append("</div>\n");

        switch (section.getType()) {
            case BizConstant.SECTION_TYPE_PROFILE -> sb.append(renderProfile(section.dataAsMap(), options));
            case BizConstant.SECTION_TYPE_EDUCATION -> sb.append(renderEducation(section.dataAsItems()));
            case BizConstant.SECTION_TYPE_WORK -> sb.append(renderWork(section.dataAsItems()));
            case BizConstant.SECTION_TYPE_PROJECT -> sb.append(renderProject(section.dataAsItems()));
            case BizConstant.SECTION_TYPE_SKILL -> sb.append(renderSkill(section.dataAsItems()));
            case BizConstant.SECTION_TYPE_INTRODUCTION -> sb.append(renderIntroduction(section.dataAsMap()));
            default -> sb.append("");
        }

        sb.append("    </div>\n");
        return sb.toString();
    }

    private String renderProfile(Map<String, Object> profile, RenderOptions options) {
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

        // 分享页隐私：隐藏联系方式（手机/邮箱/个人链接）
        if (options != null && options.hideContact()) {
            phone = "";
            email = "";
            personalWebsite = "";
            github = "";
            portfolio = "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("      <div class=\"profile-header\">\n");
        if (showAvatar && StringUtils.isNotBlank(avatarUrl)) {
            String safeAvatar = safeUrl(avatarUrl);
            if (StringUtils.isNotBlank(safeAvatar)) {
                sb.append("        <img class=\"profile-avatar\" src=\"").append(escapeHtml(absolutizeUrl(safeAvatar))).append("\" alt=\"头像\">\n");
            }
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
        String safeWebsite = safeUrl(personalWebsite);
        if (StringUtils.isNotBlank(safeWebsite)) {
            links.append("<a href=\"").append(escapeHtml(safeWebsite)).append("\">个人网站</a>");
        }
        String safeGithub = safeUrl(github);
        if (StringUtils.isNotBlank(safeGithub)) {
            if (links.length() > 0) links.append(" | ");
            links.append("<a href=\"").append(escapeHtml(safeGithub)).append("\">GitHub</a>");
        }
        String safePortfolio = safeUrl(portfolio);
        if (StringUtils.isNotBlank(safePortfolio)) {
            if (links.length() > 0) links.append(" | ");
            links.append("<a href=\"").append(escapeHtml(safePortfolio)).append("\">作品集</a>");
        }
        if (links.length() > 0) {
            sb.append("          <div class=\"profile-meta\">").append(links).append("</div>\n");
        }
        sb.append("        </div>\n")
          .append("      </div>\n");
        return sb.toString();
    }

    private String renderEducation(List<Map<String, Object>> items) {
        StringBuilder sb = new StringBuilder();
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
        return sb.toString();
    }

    private String renderWork(List<Map<String, Object>> items) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> item : items) {
            sb.append("      <div class=\"item\">\n")
              .append("        <div class=\"item-header\">\n")
              .append("          <span>").append(escapeHtml(getString(item, "company", ""))).append(" · ")
              .append(escapeHtml(getString(item, "position", ""))).append("</span>\n")
              .append("          <span>").append(escapeHtml(getString(item, "startDate", ""))).append(" - ")
              .append(escapeHtml(getString(item, "endDate", ""))).append("</span>\n")
              .append("        </div>\n")
               .append(renderRichTextOrList(item, "descriptionHtml", stringList(item.get("description"))))
               .append(renderRichTextOrList(item, "achievementsHtml", stringList(item.get("achievements"))))
               .append("      </div>\n");
        }
        return sb.toString();
    }

    private String renderProject(List<Map<String, Object>> items) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> item : items) {
            sb.append("      <div class=\"item\">\n")
              .append("        <div class=\"item-header\">\n")
              .append("          <span>").append(escapeHtml(getString(item, "name", ""))).append("</span>\n")
              .append("          <span>").append(escapeHtml(getString(item, "startDate", ""))).append(" - ")
              .append(escapeHtml(getString(item, "endDate", ""))).append("</span>\n")
              .append("        </div>\n")
              .append("        <div class=\"item-sub\">").append(escapeHtml(getString(item, "role", ""))).append("</div>\n")
               .append(renderRichTextOrList(item, "descriptionHtml", stringList(item.get("description"))))
               .append(renderRichTextOrList(item, "achievementsHtml", stringList(item.get("achievements"))))
               .append("      </div>\n");
        }
        return sb.toString();
    }

    private String renderSkill(List<Map<String, Object>> items) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> item : items) {
            sb.append("      <div class=\"item\">\n")
              .append("        <div class=\"item-header\">").append(escapeHtml(getString(item, "category", ""))).append("</div>\n")
              .append("        <div>");
            List<Map<String, Object>> skills = mapList(item.get("items"));
            for (Map<String, Object> skill : skills) {
                sb.append("<span class=\"skill-tag\">").append(escapeHtml(getString(skill, "name", ""))).append("</span>");
            }
            sb.append("</div>\n")
              .append("      </div>\n");
        }
        return sb.toString();
    }

    private String renderIntroduction(Map<String, Object> intro) {
        String richContent = getString(intro, "contentHtml", "");
        if (StringUtils.isNotBlank(richContent)) {
            return "      <div class=\"rich-text\">" + RichTextSanitizer.sanitize(richContent) + "</div>\n";
        }
        return "      <p>" + escapeHtml(getString(intro, "content", "")) + "</p>\n";
    }

    private String renderRichTextOrList(Map<String, Object> item, String htmlKey, List<String> plainText) {
        String richText = getString(item, htmlKey, "");
        if (StringUtils.isNotBlank(richText)) {
            return "        <div class=\"rich-text\">" + RichTextSanitizer.sanitize(richText) + "</div>\n";
        }
        return renderDescriptionList(plainText);
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

    /**
     * URL scheme 白名单：仅允许 http/https 与站内 /uploads 相对路径，防止 javascript:/data: 注入。
     */
    private String safeUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return "";
        }
        String trimmed = url.trim();
        String lower = trimmed.toLowerCase();
        if (lower.startsWith("http://") || lower.startsWith("https://")
                || lower.startsWith("/uploads/")) {
            return trimmed;
        }
        return "";
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
    private Map<String, Object> getMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return Map.of();
    }

    /**
     * 将字段值安全转换为字符串列表（description / achievements 等），非法或 null 返回空列表。
     */
    private List<String> stringList(Object value) {
        if (!(value instanceof List<?> raw)) {
            return List.of();
        }
        List<String> result = new java.util.ArrayList<>(raw.size());
        for (Object item : raw) {
            if (item instanceof String s) {
                result.add(s);
            }
        }
        return result;
    }

    /**
     * 将字段值安全转换为对象列表（skill.items 等），过滤非 Map 元素。
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> mapList(Object value) {
        if (!(value instanceof List<?> raw)) {
            return List.of();
        }
        List<Map<String, Object>> result = new java.util.ArrayList<>(raw.size());
        for (Object item : raw) {
            if (item instanceof Map) {
                result.add((Map<String, Object>) item);
            }
        }
        return result;
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
