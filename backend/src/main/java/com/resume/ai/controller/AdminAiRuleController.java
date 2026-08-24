package com.resume.ai.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.resume.ai.dto.AiRuleRequest;
import com.resume.ai.dto.AiRuleResponse;
import com.resume.ai.dto.AiRuleStatsResponse;
import com.resume.ai.service.AiRuleService;
import com.resume.common.entity.R;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理端 AI 规则接口（需 ADMIN 角色）。
 */
@Validated
@RestController
@RequestMapping("/admin/ai-rules")
@RequiredArgsConstructor
public class AdminAiRuleController {

    private final AiRuleService aiRuleService;

    @GetMapping
    public R<Page<AiRuleResponse>> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String ruleType,
            @RequestParam(required = false) String status) {
        return R.success(aiRuleService.list(page, size, keyword, ruleType, status));
    }

    @GetMapping("/stats")
    public R<AiRuleStatsResponse> stats() {
        return R.success(aiRuleService.stats());
    }

    @GetMapping("/{id}")
    public R<AiRuleResponse> get(@PathVariable String id) {
        return R.success(aiRuleService.get(id));
    }

    @PostMapping
    public R<AiRuleResponse> create(@AuthenticationPrincipal String operatorId,
                                    @Valid @RequestBody AiRuleRequest request) {
        return R.success(aiRuleService.create(operatorId, request));
    }

    @PutMapping("/{id}/draft")
    public R<AiRuleResponse> saveDraft(@AuthenticationPrincipal String operatorId,
                                       @PathVariable String id,
                                       @Valid @RequestBody AiRuleRequest request) {
        return R.success(aiRuleService.saveDraft(operatorId, id, request));
    }

    @PostMapping("/{id}/publish")
    public R<AiRuleResponse> publish(@AuthenticationPrincipal String operatorId,
                                     @PathVariable String id,
                                     @RequestBody(required = false) @Valid AiRuleRequest request) {
        return R.success(aiRuleService.publish(operatorId, id, request));
    }

    @PatchMapping("/{id}/status")
    public R<AiRuleResponse> toggle(@AuthenticationPrincipal String operatorId,
                                    @PathVariable String id,
                                    @RequestBody Map<String, String> body) {
        return R.success(aiRuleService.toggle(operatorId, id, body.get("status")));
    }

    @GetMapping("/{id}/versions")
    public R<List<AiRuleResponse>> versions(@PathVariable String id) {
        return R.success(aiRuleService.versions(id));
    }

    @PostMapping("/{id}/rollback/{version}")
    public R<AiRuleResponse> rollback(@AuthenticationPrincipal String operatorId,
                                      @PathVariable String id,
                                      @PathVariable @Min(1) int version) {
        return R.success(aiRuleService.rollback(operatorId, id, version));
    }

    @PostMapping("/{id}/test")
    public R<Map<String, Object>> testRun(@PathVariable String id,
                                          @RequestBody(required = false) Map<String, String> body) {
        return R.success(aiRuleService.testRun(id, body == null ? null : body.get("sampleInput")));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable String id) {
        aiRuleService.delete(id);
        return R.success();
    }
}