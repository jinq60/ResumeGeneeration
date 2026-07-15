package com.resume.resume.dto.section;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Project 模块数据。
 */
@Data
public class ProjectData {

    @NotBlank(message = "项目名称不能为空")
    @Size(max = 100, message = "项目名称不能超过100字符")
    private String name;

    @Size(max = 50, message = "项目角色不能超过50字符")
    private String role;

    @Pattern(regexp = "research|course|enterprise|competition|open_source|personal|other", 
             message = "项目类型必须是 research/course/enterprise/competition/open_source/personal/other")
    private String type;

    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "开始日期格式必须为 YYYY-MM")
    private String startDate;

    @Pattern(regexp = "^\\d{4}-\\d{2}$|^present$", message = "结束日期格式必须为 YYYY-MM 或 present")
    private String endDate;

    private List<String> techStack;

    @Size(max = 500, message = "项目背景不能超过500字符")
    private String background;

    @Size(max = 500, message = "个人职责不能超过500字符")
    private String responsibility;

    private List<String> achievements;

    @NotEmpty(message = "项目描述不能为空")
    @Size(min = 3, max = 5, message = "项目描述需要3-5条")
    private List<@NotBlank(message = "描述条目不能为空") @Size(max = 200, message = "每条描述不能超过200字符") String> description;

    @Pattern(regexp = "^https?://.*", message = "项目链接必须是有效的URL")
    private String link;

    @Pattern(regexp = "^https?://.*", message = "GitHub链接必须是有效的URL")
    private String github;
}
