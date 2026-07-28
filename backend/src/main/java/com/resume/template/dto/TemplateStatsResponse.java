package com.resume.template.dto;

import lombok.Data;

/**
 * 后台模板统计响应。
 */
@Data
public class TemplateStatsResponse {

    private Long totalTemplates;
    private Long activeTemplates;
    private Long inactiveTemplates;
    private Long builtinTemplates;
}
