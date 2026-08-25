package com.resume.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.ai.config.AiPromptTemplates;
import com.resume.ai.config.AiProperties;
import com.resume.ai.dto.AiChatRequest;
import com.resume.ai.dto.AiChatResponse;
import com.resume.ai.entity.AiCallLog;
import com.resume.ai.mapper.AiCallLogMapper;
import com.resume.ai.provider.LlmProvider;
import com.resume.ai.provider.ProviderRouter;
import com.resume.ai.util.AiCallLogDefaults;
import com.resume.common.constant.BizConstant;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.resume.entity.Resume;
import com.resume.resume.entity.ResumeReview;
import com.resume.resume.entity.ResumeReviewSuggestion;
import com.resume.resume.mapper.ResumeReviewMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 简历点评服务 —— 异步调用 LLM，未配置时回退到占位数据。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiResumeReviewService {

    private static final String FEATURE_KEY = "resume-review";

    private final ResumeReviewMapper resumeReviewMapper;
    private final AiCallLogMapper aiCallLogMapper;
    private final ProviderRouter providerRouter;
    private final AiPromptTemplates promptTemplates;
    private final ObjectMapper objectMapper;
    private final com.resume.notification.service.NotificationService notificationService;

    @Async("aiTaskExecutor")
    public void executeReview(String reviewId, Resume resume, String jobDescription) {
        ResumeReview review = resumeReviewMapper.selectById(reviewId);
        if (review == null) {
            log.error("Review not found: {}", reviewId);
            return;
        }

        review.setStatus(BizConstant.TASK_STATUS_PROCESSING);
        review.setUpdatedAt(LocalDateTime.now());
        resumeReviewMapper.updateById(review);

        try {
            LlmProvider provider = providerRouter.resolve(FEATURE_KEY);
            String model = providerRouter.resolveModel(FEATURE_KEY);
            doRealReview(review, resume, jobDescription, provider, model);
        } catch (BusinessException e) {
            if (e.getErrorCode() == ResultCode.AI_PROVIDER_NOT_CONFIGURED) {
                log.warn("AI provider not configured, using placeholder: {}", e.getMessage());
                doPlaceholderReview(review);
            } else {
                log.warn("AI review failed for reviewId={}", reviewId, e);
                review.setStatus(BizConstant.REVIEW_STATUS_FAILED);
                review.setErrorMsg(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            }
        } catch (Throwable e) {
            log.error("AI review failed for reviewId={}", reviewId, e);
            review.setStatus(BizConstant.REVIEW_STATUS_FAILED);
            review.setErrorMsg(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        } finally {
            // 兜底：若状态未被显式设置，标记为 failed，避免任务永久卡在 processing
            if (BizConstant.TASK_STATUS_PROCESSING.equals(review.getStatus())) {
                log.error("Review ended in processing state, marking failed: reviewId={}", reviewId);
                review.setStatus(BizConstant.REVIEW_STATUS_FAILED);
                review.setErrorMsg("任务执行异常结束");
            }
            review.setUpdatedAt(LocalDateTime.now());
            resumeReviewMapper.updateById(review);

            if (BizConstant.REVIEW_STATUS_SUCCESS.equals(review.getStatus())
                    && resume.getUserId() != null) {
                notificationService.notify(resume.getUserId(), "ai", "AI 点评完成",
                        "你的简历《" + resume.getTitle() + "》点评已生成，综合评分 " + review.getOverallScore() + " 分，快去查看吧。");
            } else if (BizConstant.REVIEW_STATUS_FAILED.equals(review.getStatus())
                    && resume.getUserId() != null) {
                String reason = StringUtils.abbreviate(
                        StringUtils.defaultString(review.getErrorMsg(), "未知原因"), 100);
                notificationService.notify(resume.getUserId(), "ai", "AI 点评失败",
                        "你的简历《" + resume.getTitle() + "》点评失败：" + reason + "，请稍后重试。");
            }
        }
    }

    private void doRealReview(ResumeReview review, Resume resume, String jobDescription,
                                LlmProvider provider, String model) {
        long start = System.currentTimeMillis();
        log.info("AI review chat -> provider={}, model={}, reviewId={}",
                provider.getProviderName(), model, review.getId());
        AiCallLog callLog = new AiCallLog();
        callLog.setUserId(resume.getUserId());
        callLog.setFeatureKey(FEATURE_KEY);
        callLog.setCreatedAt(LocalDateTime.now());

        try {
            String resumeContent = toJson(resume.getSections());
            String prompt = promptTemplates.render(FEATURE_KEY, Map.of(
                    "resumeContent", resumeContent,
                    "jobDescription", jobDescription != null ? jobDescription : ""
            ));

            AiProperties.FeatureConfig featureConfig = providerRouter.getFeatureConfig(FEATURE_KEY);
            AiChatRequest request = AiChatRequest.builder()
                    .model(model)
                    .userPrompt(prompt)
                    .temperature(0.3)
                    .maxTokens(4096)
                    .timeout(featureConfig.getTimeout())
                    .retry(featureConfig.getRetry())
                    .build();

            AiChatResponse response = provider.chat(request);

            callLog.setProviderName(provider.getProviderName());
            callLog.setModelName(model);
            callLog.setRequestHash(DigestUtils.md5DigestAsHex(prompt.getBytes(StandardCharsets.UTF_8)));
            callLog.setPromptTokens(response.getPromptTokens());
            callLog.setCompletionTokens(response.getCompletionTokens());
            callLog.setTotalTokens(response.getTotalTokens());
            callLog.setLatencyMs(response.getLatencyMs());
            callLog.setSuccess(response.isSuccess());

            if (response.isSuccess()) {
                ReviewResult result = parseReviewResult(response.getContent());
                review.setOverallScore(result.overallScore);
                review.setDimensionScores(result.dimensionScores);
                review.setSuggestions(result.suggestions);
                review.setHighlights(result.highlights);
                review.setModelName(model);
                review.setStatus(BizConstant.REVIEW_STATUS_SUCCESS);
            } else {
                review.setStatus(BizConstant.REVIEW_STATUS_FAILED);
                review.setErrorMsg(response.getErrorMsg());
                callLog.setErrorMsg(response.getErrorMsg());
            }
        } catch (BusinessException e) {
            callLog.setSuccess(false);
            callLog.setErrorMsg(e.getMessage());
            callLog.setLatencyMs(System.currentTimeMillis() - start);
            if (e.getErrorCode() == ResultCode.AI_PROVIDER_NOT_CONFIGURED) {
                // 仅供应商未配置时回退占位结果，避免假成功冒充真实点评
                log.warn("AI provider not configured, falling back to placeholder", e);
                doPlaceholderReview(review);
            } else {
                // 解析失败等业务异常视为点评失败，落库 failed 状态
                log.error("AI review business failure for reviewId={}", review.getId(), e);
                review.setStatus(BizConstant.REVIEW_STATUS_FAILED);
                review.setErrorMsg(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            }
        } catch (Exception e) {
            // 供应商调用失败不再伪装成 SUCCESS 占位结果
            log.error("AI call failed, marking review as failed: reviewId={}", review.getId(), e);
            review.setStatus(BizConstant.REVIEW_STATUS_FAILED);
            review.setErrorMsg(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            callLog.setSuccess(false);
            callLog.setErrorMsg(e.getMessage());
            callLog.setLatencyMs(System.currentTimeMillis() - start);
        }

        // 失败调用也要落审计表：NOT NULL 列兜底，避免 resolve 阶段异常时插入失败被吞
        AiCallLogDefaults.fillRequiredColumns(callLog, provider.getProviderName());
        aiCallLogMapper.insert(callLog);
    }

    private void doPlaceholderReview(ResumeReview review) {
        review.setOverallScore(78);
        review.setDimensionScores(buildDimensionScores());
        review.setSuggestions(buildSuggestions());
        review.setHighlights(List.of("教育背景与目标岗位匹配度高"));
        review.setModelName("placeholder");
        review.setStatus(BizConstant.REVIEW_STATUS_SUCCESS);
    }

    private ReviewResult parseReviewResult(String content) {
        try {
            String cleaned = content.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("```\\w*\\n?", "").replaceAll("```\\n?", "").trim();
            }
            JsonNode root = objectMapper.readTree(cleaned);
            ReviewResult result = new ReviewResult();
            result.overallScore = root.get("overallScore").asInt();
            result.dimensionScores = objectMapper.convertValue(root.get("dimensionScores"), Map.class);
            result.highlights = objectMapper.convertValue(root.get("highlights"), List.class);
            result.suggestions = objectMapper.convertValue(root.get("suggestions"),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ResumeReviewSuggestion.class));
            return result;
        } catch (Exception e) {
            log.error("Failed to parse review result", e);
            throw new BusinessException(ResultCode.AI_RESPONSE_PARSE_FAILED, "AI 点评结果解析失败。");
        }
    }

    private Map<String, Integer> buildDimensionScores() {
        Map<String, Integer> scores = new LinkedHashMap<>();
        scores.put("completeness", 85);
        scores.put("structure", 80);
        scores.put("content", 70);
        scores.put("match", 75);
        scores.put("expression", 82);
        return scores;
    }

    private List<ResumeReviewSuggestion> buildSuggestions() {
        ResumeReviewSuggestion suggestion = new ResumeReviewSuggestion();
        suggestion.setSectionType("project");
        suggestion.setTitle("项目经历描述不够量化");
        suggestion.setProblem("缺少具体数据和成果，招聘方难以评估贡献度。");
        suggestion.setAdvice("建议使用 STAR 法则，补充 QPS、用户数、性能提升百分比等指标。");
        suggestion.setPriority("high");
        return List.of(suggestion);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private static class ReviewResult {
        int overallScore;
        Map<String, Integer> dimensionScores;
        List<ResumeReviewSuggestion> suggestions;
        List<String> highlights;
    }
}
