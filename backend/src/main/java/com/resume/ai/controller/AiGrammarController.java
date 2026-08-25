package com.resume.ai.controller;

import com.resume.ai.dto.GrammarCheckResponse;
import com.resume.ai.service.AiGrammarService;
import com.resume.common.entity.R;
import com.resume.user.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
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
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/{resumeId}/grammar-check")
    public R<GrammarCheckResponse> check(@AuthenticationPrincipal String userId,
                                         Authentication authentication,
                                         @PathVariable String resumeId) {
        return R.success(aiGrammarService.check(userId, resolveGuest(authentication), resumeId));
    }

    /**
     * 从 Authentication 的 credentials（原始 JWT）解析游客标志；
     * fail-closed：无法确定身份（无凭证 / 非字符串凭证 / 解析异常）时按游客配额处理。
     */
    private boolean resolveGuest(Authentication authentication) {
        if (authentication == null) {
            return true;
        }
        Object credentials = authentication.getCredentials();
        if (!(credentials instanceof String token)) {
            return true;
        }
        try {
            return jwtTokenProvider.getGuest(token);
        } catch (Exception e) {
            // 身份解析失败按游客处理，避免绕过更严格的游客配额
            return true;
        }
    }
}
