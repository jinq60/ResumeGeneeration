package com.resume.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 行内 AI 写作请求。
 */
@Data
public class ResumeAiWriteRequest {

    /** Section 类型：profile/education/work/project/skill/introduction */
    @NotBlank(message = "模块类型为必填项。")
    @Size(max = 32, message = "模块类型过长。")
    private String sectionType;

    /** 字段名（白名单校验） */
    @NotBlank(message = "字段名为必填项。")
    @Size(max = 64, message = "字段名过长。")
    private String field;

    /** 操作：generate / polish / shorten / expand / translate */
    @NotBlank(message = "AI 动作为必填项。")
    @Size(max = 32, message = "AI 动作过长。")
    private String action;

    /** 当前字段原文（服务端会与简历内容交叉校验，防注入） */
    @Size(max = 2000, message = "原文过长。")
    private String originalText;

    /** 仅 translate 必填，如 en/ja */
    @Size(max = 16, message = "目标语言过长。")
    private String targetLang;
}
