package com.resume.resume.dto.section;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * WorkExperience 模块数据。
 */
@Data
public class WorkExperienceData {

    @NotBlank(message = "公司名称不能为空")
    @Size(max = 100, message = "公司名称不能超过100字符")
    private String company;

    @Size(max = 50, message = "部门名称不能超过50字符")
    private String department;

    @NotBlank(message = "职位不能为空")
    @Size(max = 50, message = "职位不能超过50字符")
    private String position;

    @Pattern(regexp = "full_time|internship|part_time|campus_job|research_assistant|volunteer", 
             message = "工作类型必须是 full_time/internship/part_time/campus_job/research_assistant/volunteer")
    private String type;

    @Size(max = 50, message = "工作城市不能超过50字符")
    private String city;

    @NotBlank(message = "开始日期不能为空")
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "开始日期格式必须为 YYYY-MM")
    private String startDate;

    @Pattern(regexp = "^\\d{4}-\\d{2}$|^present$", message = "结束日期格式必须为 YYYY-MM 或 present")
    private String endDate;

    @NotEmpty(message = "工作内容不能为空")
    private List<@NotBlank(message = "工作内容条目不能为空") @Size(max = 200, message = "每条工作内容不能超过200字符") String> description;

    private List<String> achievements;

    private List<String> techStack;

    @Size(max = 100, message = "离职原因不能超过100字符")
    private String leaveReason;

    @JsonProperty("showLeaveReason")
    private Boolean showLeaveReason = false;
}
