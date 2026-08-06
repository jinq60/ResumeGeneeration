package com.resume.ai.controller;

import com.resume.ai.dto.GrammarCheckResponse;
import com.resume.ai.service.AiGrammarService;
import com.resume.common.entity.R;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 简历语法检查接口。
 */
@RestController
@RequestMapping("/resumes")
@RequiredArgsConstructor
public class AiGrammarController {

    private final AiGrammarService aiGrammarService;

    @PostMapping("/{resumeId}/grammar-check")
    public R<GrammarCheckResponse> check(@AuthenticationPrincipal String userId,
                                         @PathVariable String resumeId) {
        return R.success(aiGrammarService.check(userId, resumeId));
    }
}
