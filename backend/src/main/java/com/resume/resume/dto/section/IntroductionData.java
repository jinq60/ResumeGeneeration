package com.resume.resume.dto.section;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Introduction 模块数据。
 */
@Data
public class IntroductionData {

    @NotBlank(message = "自我介绍内容不能为空")
    @Size(min = 50, max = 1000, message = "自我介绍长度必须在50-1000字符之间")
    private String content;

    @Pattern(regexp = "concise_formal|tech_oriented|student|senior|postgraduate|project", 
             message = "自我介绍风格必须是有效的枚举值")
    private String style;
}
