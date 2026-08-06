package com.resume.resume.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 更新简历请求。
 */
@Data
public class UpdateResumeRequest {

    @Size(max = 128, message = "简历名称过长。")
    private String title;

    @Size(max = 32, message = "使用场景过长。")
    private String scene;

    @Size(max = 128, message = "目标岗位过长。")
    private String targetPosition;

    @Size(max = 64, message = "目标行业过长。")
    private String targetIndustry;

    @Size(max = 64, message = "模板 ID 过长。")
    private String templateId;

    @Valid
    private List<SectionDTO> sections;

    @Valid
    private RenderSettings renderSettings;
}
