package com.resume.resume.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建简历请求。
 */
@Data
public class CreateResumeRequest {

    @Size(max = 128, message = "简历名称过长。")
    private String title;

    @NotBlank(message = "使用场景为必填项。")
    @Size(max = 32, message = "使用场景过长。")
    private String scene;

    @Size(max = 128, message = "目标岗位过长。")
    private String targetPosition;

    @Size(max = 64, message = "目标行业过长。")
    private String targetIndustry;

    @NotBlank(message = "模板 ID 为必填项。")
    @Size(max = 64, message = "模板 ID 过长。")
    private String templateId;
}
