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

    @Size(max = 128, message = "目标行业过长。")
    private String targetIndustry;

    @Size(max = 64, message = "模板 ID 过长。")
    private String templateId;

    @Valid
    private List<SectionDTO> sections;

    @Valid
    private RenderSettings renderSettings;

    /**
     * 乐观锁版本号（可选，向后兼容）：
     * 传入时按 CAS 更新（与库中 version 不匹配返回 2012 冲突）；
     * 不传时保持"读最新实体→写回"的旧行为。
     */
    private Integer version;
}
