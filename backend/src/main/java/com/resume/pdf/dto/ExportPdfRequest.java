package com.resume.pdf.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * PDF 导出请求。
 */
@Data
public class ExportPdfRequest {

    @NotBlank(message = "简历 ID 为必填项。")
    @Size(max = 64, message = "简历 ID 过长。")
    private String resumeId;

    @Size(max = 64, message = "模板 ID 过长。")
    private String templateId;
}
