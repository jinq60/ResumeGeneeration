package com.resume.resume.dto.section;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Profile 模块数据。
 */
@Data
public class ProfileData {

    @Size(min = 1, max = 50, message = "姓名长度必须在1-50字符之间")
    private String name;

    @Pattern(regexp = "male|female|other", message = "性别必须是 male/female/other")
    private String gender;

    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "出生日期格式必须为 YYYY-MM")
    private String birthDate;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Size(max = 50, message = "城市长度不能超过50字符")
    private String city;

    @Size(max = 100, message = "目标岗位不能超过100字符")
    private String targetPosition;

    @Size(max = 50, message = "期望薪资不能超过50字符")
    private String expectedSalary;

    @Size(max = 50, message = "到岗时间不能超过50字符")
    private String availability;

    @Pattern(regexp = "^https?://.*", message = "个人网站必须是有效的URL")
    private String personalWebsite;

    @Pattern(regexp = "^https?://.*", message = "GitHub链接必须是有效的URL")
    private String github;

    @Pattern(regexp = "^https?://.*", message = "作品集链接必须是有效的URL")
    private String portfolio;

    @Pattern(regexp = "^https?://.*", message = "头像URL必须是有效的URL")
    private String avatarUrl;

    @JsonProperty("showGender")
    private Boolean showGender = true;

    @JsonProperty("showAge")
    private Boolean showAge = false;

    @JsonProperty("showSalary")
    private Boolean showSalary = false;

    @JsonProperty("showAvatar")
    private Boolean showAvatar = true;

    /**
     * 被眼睛隐藏的字段 key 列表（hiddenFields），用于简历显隐而非删除。
     * 前端 ProfileForm 通过眼睛切换，行保留但简历预览/PDF 不渲染对应字段。
     * 合法 key：name, targetPosition, availability, birthDate, email, phone, city,
     * expectedSalary, personalWebsite, github, portfolio, gender 等；未知 key 忽略。
     */
    private List<String> hiddenFields;
}
