package com.resume.resume.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 简历导入请求（JSON 或 Markdown）。
 */
@Data
public class ResumeImportRequest {

    @Size(max = 128, message = "标题过长。")
    private String title;

    @Pattern(regexp = "campus_recruitment|internship|social_recruitment|postgraduate_reexam|project_application|custom",
            message = "使用场景不合法。")
    private String scene = "campus_recruitment";

    @Size(max = 128, message = "目标岗位过长。")
    private String targetPosition;

    @NotBlank(message = "请选择模板。")
    private String templateId;

    @NotBlank(message = "请选择导入格式。")
    @Pattern(regexp = "json|markdown", message = "仅支持 json 或 markdown 格式。")
    private String format;

    @NotBlank(message = "导入内容不能为空。")
    @Size(max = 200000, message = "导入内容过长。")
    private String content;
}
