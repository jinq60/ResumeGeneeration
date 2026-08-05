package com.resume.ai.controller;

import com.resume.ai.dto.ResumeAiWriteRequest;
import com.resume.ai.dto.ResumeAiWriteResponse;
import com.resume.ai.service.AiWritingService;
import com.resume.common.entity.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 行内 AI 写作接口（编辑器字段级生成/润色/翻译）。
 */
@RestController
@RequestMapping("/resumes")
@RequiredArgsConstructor
public class AiWritingController {

    private final AiWritingService aiWritingService;

    @PostMapping("/{resumeId}/ai/write")
    public R<ResumeAiWriteResponse> write(@AuthenticationPrincipal String userId,
                                          @PathVariable String resumeId,
                                          @Valid @RequestBody ResumeAiWriteRequest request) {
        return R.success(aiWritingService.write(userId, resumeId, request));
    }
}
