package com.resume.resume.dto.section;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Skill 模块数据。
 */
@Data
public class SkillData {

    @NotBlank(message = "技能分类不能为空")
    @Pattern(regexp = "programming_language|frontend|backend|database|ai_data|design|office|language|professional_tool|other", 
             message = "技能分类必须是有效的枚举值")
    private String category;

    @NotEmpty(message = "技能项不能为空")
    private List<SkillItem> items;

    @Data
    public static class SkillItem {
        @NotBlank(message = "技能名称不能为空")
        @Size(max = 50, message = "技能名称不能超过50字符")
        private String name;

        @Pattern(regexp = "beginner|familiar|proficient|expert", 
                 message = "熟练程度必须是 beginner/familiar/proficient/expert")
        private String level;

        private Boolean highlight = false;
    }
}
