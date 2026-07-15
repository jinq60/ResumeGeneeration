package com.resume.resume.dto.section;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Education 模块数据。
 */
@Data
public class EducationData {

    @NotBlank(message = "学校名称不能为空")
    @Size(max = 100, message = "学校名称不能超过100字符")
    private String school;

    @NotBlank(message = "学历不能为空")
    @Size(max = 50, message = "学历不能超过50字符")
    private String degree;

    @NotBlank(message = "专业不能为空")
    @Size(max = 100, message = "专业不能超过100字符")
    private String major;

    @Size(max = 100, message = "学院名称不能超过100字符")
    private String college;

    @NotBlank(message = "开始日期不能为空")
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "开始日期格式必须为 YYYY-MM")
    private String startDate;

    @Pattern(regexp = "^\\d{4}-\\d{2}$|^present$", message = "结束日期格式必须为 YYYY-MM 或 present")
    private String endDate;

    @Size(max = 20, message = "GPA不能超过20字符")
    private String gpa;

    @Size(max = 50, message = "排名不能超过50字符")
    private String rank;

    private List<String> honors;

    private List<String> courses;
}
