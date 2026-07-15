package com.resume.resume.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * AI 简历点评请求。
 */
@Data
public class ReviewResumeRequest {

    @Size(max = 5000, message = "岗位描述过长。")
    private String jobDescription;

    private List<String> focusAreas;
}
