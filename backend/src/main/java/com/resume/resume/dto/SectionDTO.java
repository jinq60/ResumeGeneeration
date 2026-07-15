package com.resume.resume.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Section 统一结构。
 */
@Data
public class SectionDTO {

    @NotBlank(message = "模块 ID 为必填项。")
    private String id;

    @NotBlank(message = "模块类型为必填项。")
    private String type;

    @NotBlank(message = "模块标题为必填项。")
    private String title;

    @NotNull(message = "模块排序为必填项。")
    private Integer order;

    @NotNull(message = "模块展示状态为必填项。")
    private Boolean visible;

    @NotNull(message = "模块数据不能为空。")
    @Valid
    private Object data;
}
