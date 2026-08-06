package com.resume.common.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.resume.dto.RenderSettings;
import com.resume.resume.dto.SectionDTO;
import com.resume.resume.entity.Resume;
import com.resume.template.entity.Template;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ResumeRenderServiceTest {

    private final ResumeRenderService renderService = new ResumeRenderService(new ObjectMapper());

    @Test
    void render_shouldIncludeAvatarAndExtraProfileFields() throws Exception {
        Resume resume = new Resume();
        resume.setId("resume_1");

        Map<String, Object> profileData = new HashMap<>();
        profileData.put("name", "张三");
        profileData.put("phone", "13800000000");
        profileData.put("email", "zhangsan@example.com");
        profileData.put("city", "北京");
        profileData.put("targetPosition", "Java 开发工程师");
        profileData.put("showAvatar", true);
        profileData.put("avatarUrl", "/uploads/avatars/user_1/avatars/avatar_1_source.png");
        profileData.put("showGender", true);
        profileData.put("gender", "男");
        profileData.put("showAge", true);
        profileData.put("age", "28");
        profileData.put("showSalary", true);
        profileData.put("expectedSalary", "20k-30k");

        SectionDTO section = new SectionDTO();
        section.setId("sec_profile");
        section.setType("profile");
        section.setTitle("个人信息");
        section.setOrder(0);
        section.setVisible(true);
        section.setData(profileData);

        resume.setSections(List.of(section));

        Template template = new Template();
        template.setId("template_1");
        // htmlTemplate 留空 -> 触发回退到内置单栏渲染器
        template.setHtmlTemplate(null);
        template.setConfig("{}");

        String html = renderService.render(resume, template);

        assertAll(
            () -> assertTrue(html.contains("张三")),
            () -> assertTrue(html.contains("Java 开发工程师")),
            () -> assertTrue(html.contains("13800000000")),
            () -> assertTrue(html.contains("zhangsan@example.com")),
            () -> assertTrue(html.contains("北京")),
            () -> assertTrue(html.contains("profile-avatar")),
            () -> assertTrue(html.contains("/uploads/avatars/user_1/avatars/avatar_1_source.png")),
            () -> assertTrue(html.contains("男")),
            () -> assertTrue(html.contains("28岁")),
            () -> assertTrue(html.contains("20k-30k"))
        );
    }

    @Test
    void render_shouldLoadSkeletonFromHtmlTemplateFile() {
        Resume resume = new Resume();
        resume.setId("resume_1");
        resume.setTitle("我的简历");

        Map<String, Object> profileData = new HashMap<>();
        profileData.put("name", "李四");
        SectionDTO section = new SectionDTO();
        section.setId("sec_profile");
        section.setType("profile");
        section.setTitle("个人信息");
        section.setOrder(0);
        section.setVisible(true);
        section.setData(profileData);
        resume.setSections(List.of(section));

        Template template = new Template();
        template.setId("template_classic_single");
        // 指向 classpath:templates/resume/classic-single.html
        template.setHtmlTemplate("classic-single");
        template.setConfig("{\"color\":{\"primary\":\"#333333\",\"accent\":\"#1a5276\"}}");

        String html = renderService.render(resume, template);

        // 来自 skeleton 文件的 title 占位被替换
        assertTrue(html.contains("<title>我的简历</title>"));
        // 来自 skeleton 文件，body 占位会被替换为 section 内容
        assertTrue(html.contains("李四"));
        // CSS 占位被替换为模板配置生成的 CSS
        assertTrue(html.contains("#1a5276"));
    }

    @Test
    void render_shouldPrefixUploadsWithPublicBaseUrl() throws Exception {
        ResumeRenderService renderService = new ResumeRenderService(new ObjectMapper());
        org.springframework.test.util.ReflectionTestUtils.setField(renderService, "publicBaseUrl", "http://localhost");

        Resume resume = new Resume();
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("name", "王五");
        profileData.put("showAvatar", true);
        profileData.put("avatarUrl", "/uploads/avatars/user_1/avatars/a.png");
        SectionDTO section = new SectionDTO();
        section.setId("sec_profile");
        section.setType("profile");
        section.setTitle("个人信息");
        section.setOrder(0);
        section.setVisible(true);
        section.setData(profileData);
        resume.setSections(List.of(section));

        Template template = new Template();
        template.setHtmlTemplate(null);
        template.setConfig("{}");

        String html = renderService.render(resume, template);

        assertTrue(html.contains("src=\"http://localhost/uploads/avatars/user_1/avatars/a.png\""));
    }

    @Test
    void render_shouldKeepRelativeUploadsUrlWhenBaseUrlNotConfigured() {
        Resume resume = new Resume();
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("name", "赵六");
        profileData.put("showAvatar", true);
        profileData.put("avatarUrl", "/uploads/avatars/user_1/avatars/b.png");
        SectionDTO section = new SectionDTO();
        section.setId("sec_profile");
        section.setType("profile");
        section.setTitle("个人信息");
        section.setOrder(0);
        section.setVisible(true);
        section.setData(profileData);
        resume.setSections(List.of(section));

        Template template = new Template();
        template.setHtmlTemplate(null);
        template.setConfig("{}");

        String html = renderService.render(resume, template);

        assertTrue(html.contains("src=\"/uploads/avatars/user_1/avatars/b.png\""));
    }

    @Test
    void render_shouldSanitizeAndRenderRichTextFields() {
        Resume resume = new Resume();

        Map<String, Object> introData = new HashMap<>();
        introData.put("content", "纯文本回退");
        introData.put("contentHtml", "<p><strong>专业介绍</strong></p><script>alert(1)</script>");
        SectionDTO intro = new SectionDTO();
        intro.setId("sec_intro");
        intro.setType("introduction");
        intro.setTitle("自我介绍");
        intro.setOrder(0);
        intro.setVisible(true);
        intro.setData(introData);
        resume.setSections(List.of(intro));

        Template template = new Template();
        template.setHtmlTemplate(null);
        template.setConfig("{}");

        String html = renderService.render(resume, template);

        assertTrue(html.contains("<strong>专业介绍</strong>"));
        assertFalse(html.contains("<script"));
        assertFalse(html.contains("alert(1)"));
    }

    @Test
    void render_shouldApplyResumeRenderSettingsAndOnePageMarker() {
        Resume resume = new Resume();
        resume.setRenderSettings(new RenderSettings()
                .setAutoOnePage(true)
                .setFontFamily("Arial, sans-serif")
                .setBaseFontSize(9.5)
                .setLineHeight(1.25)
                .setPagePadding(12.0)
                .setSectionSpacing(8.0)
                .setAccentColor("#d14a3a"));

        SectionDTO intro = new SectionDTO();
        intro.setId("sec_intro");
        intro.setType("introduction");
        intro.setTitle("自我介绍");
        intro.setOrder(0);
        intro.setVisible(true);
        intro.setData(Map.of("content", "内容"));
        resume.setSections(List.of(intro));

        Template template = new Template();
        template.setHtmlTemplate(null);
        template.setConfig("{}");

        String html = renderService.render(resume, template);

        assertAll(
                () -> assertTrue(html.contains("data-auto-one-page=\"true\"")),
                () -> assertTrue(html.contains("font-family: Arial, sans-serif")),
                () -> assertTrue(html.contains("font-size: 9.5pt")),
                () -> assertTrue(html.contains("line-height: 1.25")),
                () -> assertTrue(html.contains("padding: 12mm")),
                () -> assertTrue(html.contains("margin-bottom: 8px")),
                () -> assertTrue(html.contains("#d14a3a")),
                () -> assertTrue(html.contains("--resume-fit-scale"))
        );
    }

    @Test
    void render_shouldIgnoreUnsafeRenderSettings() {
        Resume resume = new Resume();
        resume.setRenderSettings(new RenderSettings()
                .setFontFamily("Arial; color: red")
                .setAccentColor("red; background: url(javascript:alert(1))"));
        resume.setSections(List.of());

        Template template = new Template();
        template.setHtmlTemplate(null);
        template.setConfig("{}");

        String html = renderService.render(resume, template);

        assertFalse(html.contains("javascript:"));
        assertFalse(html.contains("Arial; color: red"));
        assertTrue(html.contains("\"Noto Sans SC\""));
    }
}
