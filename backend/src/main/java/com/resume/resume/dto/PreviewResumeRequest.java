package com.resume.resume.dto;

import com.resume.resume.entity.Resume;
import lombok.Data;

/**
 * 实时预览请求：前端携带当前编辑中的简历对象与可选模板 ID，服务端直接渲染 HTML 返回。
 */
@Data
public class PreviewResumeRequest {

    private Resume resume;
    private String templateId;
}
