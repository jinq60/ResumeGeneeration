package com.resume.ai.controller;

import com.resume.ai.dto.ResumeAiWriteRequest;
import com.resume.ai.dto.ResumeAiWriteResponse;
import com.resume.ai.service.AiWritingService;
import com.resume.common.entity.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

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

    @PostMapping(value = "/{resumeId}/ai/write/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(@AuthenticationPrincipal String userId,
                                                @PathVariable String resumeId,
                                                @Valid @RequestBody ResumeAiWriteRequest request) {
        return aiWritingService.stream(userId, resumeId, request)
                .map(content -> ServerSentEvent.<String>builder()
                        .event("delta")
                        .data(content)
                        .build())
                .concatWithValues(ServerSentEvent.<String>builder().event("done").data("").build())
                .onErrorResume(error -> Flux.just(ServerSentEvent.<String>builder()
                        .event("error")
                        .data(error.getMessage() == null ? "AI 写作失败，请稍后重试。" : error.getMessage())
                        .build()));
    }
}
