package com.resume.common.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
}
