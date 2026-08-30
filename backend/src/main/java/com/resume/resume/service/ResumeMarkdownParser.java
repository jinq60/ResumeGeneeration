package com.resume.resume.service;

import com.resume.common.constant.BizConstant;
import com.resume.resume.dto.SectionDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Markdown 简历解析器。
 * <p>
 * 支持按模块标题（##）分块，映射到简历 Section 类型；未知标题映射为自定义模块。
 * 解析结果为准入式结构，用户可在编辑器中继续精修。
 * </p>
 */
public final class ResumeMarkdownParser {

    private static final Map<String, String> TYPE_KEYWORDS = Map.ofEntries(
            Map.entry("个人信息", BizConstant.SECTION_TYPE_PROFILE),
            Map.entry("基本信息", BizConstant.SECTION_TYPE_PROFILE),
            Map.entry("联系方式", BizConstant.SECTION_TYPE_PROFILE),
            Map.entry("教育", BizConstant.SECTION_TYPE_EDUCATION),
            Map.entry("工作经历", BizConstant.SECTION_TYPE_WORK),
            Map.entry("职业经历", BizConstant.SECTION_TYPE_WORK),
            Map.entry("实习经历", BizConstant.SECTION_TYPE_WORK),
            Map.entry("工作", BizConstant.SECTION_TYPE_WORK),
            Map.entry("项目", BizConstant.SECTION_TYPE_PROJECT),
            Map.entry("技能", BizConstant.SECTION_TYPE_SKILL),
            Map.entry("技术栈", BizConstant.SECTION_TYPE_SKILL),
            Map.entry("自我介绍", BizConstant.SECTION_TYPE_INTRODUCTION),
            Map.entry("个人简介", BizConstant.SECTION_TYPE_INTRODUCTION),
            Map.entry("个人总结", BizConstant.SECTION_TYPE_INTRODUCTION),
            Map.entry("关于我", BizConstant.SECTION_TYPE_INTRODUCTION)
    );

    private ResumeMarkdownParser() {
    }

    /**
     * 解析 Markdown 为 Section 列表。
     *
     * @param markdown 导入内容
     * @return 解析出的 Section 列表；无任何有效模块时返回空列表
     */
    public static List<SectionDTO> parse(String markdown) {
        List<SectionDTO> sections = new ArrayList<>();
        if (markdown == null || markdown.isBlank()) {
            return sections;
        }

        String[] lines = markdown.split("\\R");
        SectionBuilder current = null;
        int order = 0;

        for (String rawLine : lines) {
            String line = rawLine.strip();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("## ")) {
                if (current != null) {
                    sections.add(current.build(order++));
                }
                current = new SectionBuilder(line.substring(3).strip());
            } else if (line.startsWith("# ")) {
                // 一级标题作为简历标题，忽略
                continue;
            } else if (current != null) {
                current.addLine(line);
            }
        }
        if (current != null) {
            sections.add(current.build(order));
        }
        return sections;
    }

    /**
     * 收集中的模块块。
     */
    private static final class SectionBuilder {
        private final String rawTitle;
        private final String type;
        private final List<String> bullets = new ArrayList<>();
        private final List<String> paragraphs = new ArrayList<>();

        SectionBuilder(String rawTitle) {
            this.rawTitle = rawTitle;
            this.type = resolveType(rawTitle);
        }

        void addLine(String line) {
            if (line.startsWith("- ") || line.startsWith("* ")) {
                bullets.add(line.substring(2).strip());
            } else {
                paragraphs.add(line);
            }
        }

        SectionDTO build(int order) {
            SectionDTO section = new SectionDTO();
            section.setId("sec_" + java.util.UUID.randomUUID().toString().replace("-", "") + "_" + order);
            section.setType(type);
            section.setTitle(rawTitle);
            section.setOrder(order);
            section.setVisible(true);
            section.setData(buildData());
            return section;
        }

        private Object buildData() {
            String firstLine = paragraphs.isEmpty() ? "" : paragraphs.get(0);
            List<String> restParagraphs = paragraphs.size() > 1 ? paragraphs.subList(1, paragraphs.size()) : List.of();
            switch (type) {
                case "profile" -> {
                    Map<String, Object> profile = new java.util.HashMap<>();
                    profile.put("name", firstLine);
                    return profile;
                }
                case "education" -> {
                    Map<String, Object> item = new java.util.HashMap<>();
                    item.put("school", firstLine);
                    item.put("degree", "");
                    item.put("major", "");
                    return List.of(item);
                }
                case "work" -> {
                    Map<String, Object> item = new java.util.HashMap<>();
                    item.put("company", firstLine);
                    item.put("position", "");
                    item.put("description", new ArrayList<>(bullets));
                    return List.of(item);
                }
                case "project" -> {
                    Map<String, Object> item = new java.util.HashMap<>();
                    item.put("name", firstLine);
                    item.put("description", new ArrayList<>(bullets));
                    return List.of(item);
                }
                case "skill" -> {
                    List<Map<String, Object>> items = bullets.stream()
                            .map(name -> {
                                Map<String, Object> skill = new java.util.HashMap<>();
                                skill.put("name", name);
                                return skill;
                            })
                            .toList();
                    Map<String, Object> category = new java.util.HashMap<>();
                    // 使用白名单中的分类，避免校验失败（原 "技能" 不在 SKILL_CATEGORIES 中）
                    category.put("category", "other");
                    category.put("items", items);
                    return List.of(category);
                }
                case "introduction" -> {
                    Map<String, Object> intro = new java.util.HashMap<>();
                    intro.put("content", String.join("\n", paragraphs));
                    return intro;
                }
                default -> {
                    Map<String, Object> custom = new java.util.HashMap<>();
                    custom.put("title", rawTitle);
                    custom.put("content", String.join("\n", paragraphs));
                    return custom;
                }
            }
        }
    }

    private static String resolveType(String title) {
        String lower = title.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> entry : TYPE_KEYWORDS.entrySet()) {
            if (lower.contains(entry.getKey().toLowerCase(Locale.ROOT))) {
                return entry.getValue();
            }
        }
        return BizConstant.SECTION_TYPE_CUSTOM;
    }
}
