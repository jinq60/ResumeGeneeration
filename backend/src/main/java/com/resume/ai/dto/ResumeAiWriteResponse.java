package com.resume.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 行内 AI 写作响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumeAiWriteResponse {

    /** AI 生成的文本内容 */
    private String content;
}
