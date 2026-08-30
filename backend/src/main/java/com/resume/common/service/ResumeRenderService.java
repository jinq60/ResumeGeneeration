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
            byte[] bytes = in.readAllBytes();
            if (bytes.length > 512 * 1024) {
                log.warn("Template skeleton too large, fallback to builtin: {} size={}", path, bytes.length);
                return null;
            }
            return new String(bytes, StandardCharsets.UTF_8);
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
        String rawFontFamily = settings.getFontFamily() != null
                ? settings.getFontFamily()
                : getString(font, "family", RenderSettings.DEFAULT_FONT_FAMILY);
        String fontFamily = sanitizeFontFamily(rawFontFamily);
        String mainFontSize = settings.getBaseFontSize() != null
                ? cssNumber(settings.getBaseFontSize()) + "pt"
                : getString(font, "mainSize", "10.5pt");
        String lineHeight = settings.getLineHeight() != null
                ? cssNumber(settings.getLineHeight())
                : getString(font, "lineHeight", "1.5");
        String rawAccent = settings.getAccentColor() != null
                ? settings.getAccentColor()
                : getString(color, "accent", "#1a5276");
        String accentColor = sanitizeHexColor(rawAccent);
        String sectionSpacing = settings.getSectionSpacing() != null
                ? cssNumber(settings.getSectionSpacing()) + "px"
                : getString(config, "moduleSpacing", "16px");

        StringBuilder css = new StringBuilder();
        // 基础重置与排版，提升可读性与专业感
        css.append("    * { box-sizing: border-box; margin: 0; padding: 0; }\n")
           .append("    @page { size: A4; margin: 0; }\n")
           .append("    html, body { margin: 0; padding: 0; background: #f8f7f4; }\n")
           .append("    body { font-family: ").append(fontFamily).append("; line-height: ").append(lineHeight).append("; -webkit-font-smoothing: antialiased; }\n")
           .append("    .resume-page { width: ").append(pageWidth).append(";\n")
           .append("      min-height: ").append(pageHeight).append(";\n")
           .append("      padding: ").append(pageMargin).append(";\n")
           .append("      margin: 0 auto;\n")
           .append("      background: ").append(getString(color, "background", "#ffffff")).append(";\n")
           .append("      color: ").append(getString(color, "primary", "#1a1a1a")).append(";\n")
           .append("      font-size: ").append(mainFontSize).append("; }\n")
           .append("    .resume-page[data-auto-one-page=\"true\"] {\n")
           .append("      --resume-fit-scale: 1;\n")
           .append("      width: calc(").append(pageWidth).append(" / var(--resume-fit-scale));\n")
           .append("      min-height: calc(").append(pageHeight).append(" / var(--resume-fit-scale));\n")
           .append("      transform: scale(var(--resume-fit-scale));\n")
           .append("      transform-origin: top left;\n")
           .append("    }\n")
           // 区块与标题：更精致的分割线与字距
           .append("    .section { margin-bottom: ").append(sectionSpacing).append("; }\n")
           .append("    .section-title { font-size: 11pt; font-weight: 700; letter-spacing: 0.06em; text-transform: uppercase;\n")
           .append("      color: ").append(accentColor).append(";\n")
           .append("      border-bottom: 1.2px solid ").append(accentColor).append(";\n")
           .append("      padding-bottom: 5px; margin-bottom: 10px; }\n")
           // 个人信息头部：更紧凑且层次分明
           .append("    .profile-header { display: flex; align-items: center; gap: 18px; margin-bottom: 14px; padding-bottom: 10px; border-bottom: 1px solid #eee; }\n")
           .append("    .profile-avatar { width: 22mm; height: 22mm; object-fit: cover; border-radius: 6px; flex-shrink: 0; box-shadow: 0 1px 4px rgba(0,0,0,0.08); }\n")
           .append("    .profile-info { flex: 1; min-width: 0; }\n")
           .append("    .profile-name { font-size: 20pt; font-weight: 800; letter-spacing: -0.02em; line-height: 1.1; margin-bottom: 6px; color: #111; }\n")
           .append("    .profile-meta { color: ").append(getString(color, "secondary", "#5a5a5a")).append("; font-size: 8.5pt; margin-bottom: 3px; line-height: 1.4; }\n")
           .append("    .profile-meta a { color: ").append(accentColor).append("; text-decoration: none; border-bottom: 1px dotted ").append(accentColor).append("; }\n")
           // 条目：卡片感与时间线
           .append("    .item { margin-bottom: 13px; padding-left: 0; }\n")
           .append("    .item-header { display: flex; justify-content: space-between; align-items: baseline; font-weight: 700; font-size: 10pt; margin-bottom: 2px; }\n")
           .append("    .item-header span:last-child { font-weight: 400; color: ").append(getString(color, "secondary", "#6b7280")).append("; font-size: 8.5pt; white-space: nowrap; margin-left: 12px; }\n")
           .append("    .item-sub { color: ").append(getString(color, "secondary", "#4b5563")).append("; font-size: 8.5pt; margin-bottom: 4px; font-style: italic; }\n")
           .append("    .rich-text { font-size: 9pt; color: #2a2a2a; }\n")
           .append("    .rich-text p { margin: 0 0 5px; }\n")
           .append("    .rich-text ul, .rich-text ol { padding-left: 18px; margin: 3px 0; }\n")
           .append("    .rich-text li { margin-bottom: 2px; }\n")
           .append("    .rich-text a { color: ").append(accentColor).append("; text-decoration: underline; text-underline-offset: 2px; }\n")
           .append("    ul { padding-left: 18px; }\n")
           .append("    li { margin-bottom: 2px; font-size: 9pt; }\n")
           .append("    .skill-tag { display: inline-block; margin-right: 6px; margin-bottom: 5px;\n")
           .append("      padding: 3px 8px; background: #f3f4f6; border: 1px solid #e5e7eb; border-radius: 9999px; font-size: 8pt; font-weight: 500; color: #374151; }\n")
           .append("    .section-empty { padding: 14px 12px; border: 1px dashed #e5e7eb; border-radius: 8px; background: #f9fafb; color: #9ca3af; font-size: 8.5pt; text-align: center; letter-spacing: 0.02em; }\n");
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
        String safeFont = sanitizeFontFamily(settings.getFontFamily());
        if (safeFont != null) {
            override.append("body, .resume-page { font-family: ")
                    .append(safeFont).append(" !important; }\n");
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
        String safeAccent = sanitizeHexColor(settings.getAccentColor());
        if (safeAccent != null) {
            override.append(".section-title, .rich-text a { color: ")
                    .append(safeAccent).append(" !important; }\n")
                    .append(".section-title { border-color: ")
                    .append(safeAccent).append(" !important; }\n")
                    .append(".resume-page { --resume-accent-color: ")
                    .append(safeAccent).append("; }\n");
        }
        return override.toString();
    }

    private String cssNumber(Double value) {
        if (value == null) {
            return "0";
        }
        return value % 1 == 0 ? String.valueOf(value.intValue()) : String.valueOf(value);
    }

    private static String sanitizeHexColor(String color) {
        if (color == null) {
            return null;
        }
        String trimmed = color.trim();
        return trimmed.matches("^#[0-9a-fA-F]{6}$") ? trimmed : null;
    }

    private static String sanitizeFontFamily(String fontFamily) {
        if (fontFamily == null) {
            return null;
        }
        // 仅允许 RenderSettings 白名单中的字体（防御模板配置投毒）
        java.util.Set<String> allowed = java.util.Set.of(
                RenderSettings.DEFAULT_FONT_FAMILY,
                "Arial, sans-serif",
                "\"Source Han Sans SC\", \"Noto Sans SC\", sans-serif",
                "\"SimSun\", serif"
        );
        return allowed.contains(fontFamily) ? fontFamily : null;
    }

    private String renderSection(SectionDTO section, RenderOptions options) {
        // 空数据时展示柔和占位，避免大面积留白显得“差劲”（与 magic-resume 空状态一致）
        boolean isEmpty = isSectionEmpty(section);
        StringBuilder sb = new StringBuilder();
        sb.append("    <div class=\"section\">\n")
          .append("      <div class=\"section-title\">").append(escapeHtml(section.getTitle())).append("</div>\n");

        if (isEmpty) {
            sb.append("      <div class=\"section-empty\">暂无内容 — 在左侧添加</div>\n");
        } else {
            switch (section.getType()) {
                case BizConstant.SECTION_TYPE_PROFILE -> sb.append(renderProfile(section.dataAsMap(), options));
                case BizConstant.SECTION_TYPE_EDUCATION -> sb.append(renderEducation(section.dataAsItems()));
                case BizConstant.SECTION_TYPE_WORK -> sb.append(renderWork(section.dataAsItems()));
                case BizConstant.SECTION_TYPE_PROJECT -> sb.append(renderProject(section.dataAsItems()));
                case BizConstant.SECTION_TYPE_SKILL -> sb.append(renderSkill(section.dataAsItems()));
                case BizConstant.SECTION_TYPE_INTRODUCTION -> sb.append(renderIntroduction(section.dataAsMap()));
                default -> sb.append("");
            }
        }

        sb.append("    </div>\n");
        return sb.toString();
    }

    private boolean isSectionEmpty(SectionDTO section) {
        Object data = section.getData();
        if (data == null) return true;
        if (data instanceof Map<?,?> m) return m.isEmpty() || m.values().stream().allMatch(v -> v == null || v.toString().isBlank() || (v instanceof java.util.Collection<?> c && c.isEmpty()));
        if (data instanceof java.util.Collection<?> c) return c.isEmpty();
        return false;
    }

    private String renderProfile(Map<String, Object> profile, RenderOptions options) {
        // hiddenFields：眼睛显隐（非删除），行保留但简历不渲染对应字段；未知 key 忽略
        java.util.Set<String> hidden = toHiddenSet(profile.get("hiddenFields"));
        boolean showAvatar = getBoolean(profile, "showAvatar", true);
        String avatarUrl = hidden.contains("avatarUrl") || hidden.contains("avatar") ? "" : getString(profile, "avatarUrl", "");
        if (hidden.contains("avatar")) {
            showAvatar = false;
        }
        String name = hidden.contains("name") ? "" : getString(profile, "name", "");
        String phone = hidden.contains("phone") ? "" : getString(profile, "phone", "");
        String email = hidden.contains("email") ? "" : getString(profile, "email", "");
        String city = hidden.contains("city") ? "" : getString(profile, "city", "");
        String targetPosition = hidden.contains("targetPosition") ? "" : getString(profile, "targetPosition", "");
        boolean showGender = getBoolean(profile, "showGender", false);
        if (hidden.contains("gender")) {
            showGender = false;
        }
        String gender = getString(profile, "gender", "");
        boolean showAge = getBoolean(profile, "showAge", false);
        if (hidden.contains("age")) {
            showAge = false;
        }
        String age = getString(profile, "age", "");
        boolean showSalary = getBoolean(profile, "showSalary", false);
        if (hidden.contains("expectedSalary") || hidden.contains("salary")) {
            showSalary = false;
        }
        String expectedSalary = getString(profile, "expectedSalary", "");
        String availability = hidden.contains("availability") ? "" : getString(profile, "availability", "");
        String personalWebsite = hidden.contains("personalWebsite") ? "" : getString(profile, "personalWebsite", "");
        String github = hidden.contains("github") ? "" : getString(profile, "github", "");
        String portfolio = hidden.contains("portfolio") ? "" : getString(profile, "portfolio", "");
        // birthDate 在当前内置模板未在 header 单独渲染，但预留显隐能力（清空后模板若引用则不显示）
        String birthDate = hidden.contains("birthDate") ? "" : getString(profile, "birthDate", "");

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

    @SuppressWarnings("unchecked")
    private java.util.Set<String> toHiddenSet(Object value) {
        if (!(value instanceof List<?> raw)) {
            return java.util.Set.of();
        }
        java.util.Set<String> set = new java.util.HashSet<>();
        for (Object item : raw) {
            if (item instanceof String s && !s.isBlank()) {
                set.add(s.trim());
            }
        }
        return set;
    }
}
