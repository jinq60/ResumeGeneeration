package com.resume.resume.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 重命名简历请求。
 */
@Data
public class RenameResumeRequest {

    @NotBlank(message = "简历名称为必填项。")
    @Size(max = 128, message = "简历名称过长。")
    private String title;
}
