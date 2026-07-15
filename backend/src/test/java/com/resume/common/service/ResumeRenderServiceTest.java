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
}
