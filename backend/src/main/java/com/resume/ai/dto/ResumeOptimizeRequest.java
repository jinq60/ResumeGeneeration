package com.resume.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * JD 匹配优化请求。
 */
@Data
public class ResumeOptimizeRequest {

    @NotBlank(message = "岗位描述为必填项。")
    @Size(max = 5000, message = "岗位描述过长。")
    private String jobDescription;

    private List<String> focusSections;
}
