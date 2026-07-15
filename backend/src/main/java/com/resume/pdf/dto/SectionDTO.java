package com.resume.pdf.dto;

import lombok.Data;

/**
 * PDF 渲染用 Section 结构。
 */
@Data
public class SectionDTO {

    private String id;
    private String type;
    private String title;
    private Integer order;
    private Boolean visible;
    private Object data;
}
