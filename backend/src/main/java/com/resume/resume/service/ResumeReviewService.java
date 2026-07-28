package com.resume.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.resume.ai.service.AiResumeReviewService;
import com.resume.common.constant.BizConstant;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.resume.dto.ResumeReviewResponse;
import com.resume.resume.dto.ReviewResumeRequest;
import com.resume.resume.dto.SectionDTO;
import com.resume.resume.entity.Resume;
import com.resume.resume.entity.ResumeReview;
import com.resume.resume.entity.ResumeReviewSuggestion;
import com.resume.resume.mapper.ResumeMapper;
import com.resume.resume.mapper.ResumeReviewMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * AI 简历点评服务。
 * <p>
 * 优先尝试异步调用真实 AI；若 AI 供应商未配置则回退到 P0 占位实现。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeReviewService {

    private final ResumeReviewMapper resumeReviewMapper;
    private final ResumeMapper resumeMapper;
    private final AiResumeReviewService aiResumeReviewService;

    /**
     * 创建 AI 简历点评（异步）。
     * <p>
     * 创建 pending 状态的点评记录后立即返回，由
     * {@link AiResumeReviewService#executeReview} 异步调用 LLM 并更新结果。
     * 前端应轮询 GET /resumes/{id}/reviews/latest 获取最终结果。
     * </p>
     */
    @Transactional(rollbackFor = Exception.class)
    public ResumeReviewResponse reviewResume(String userId, String resumeId, ReviewResumeRequest request) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null || BizConstant.DELETED.equals(resume.getDeleted())) {
            throw new BusinessException(ResultCode.RESUME_NOT_FOUND, "简历不存在。");
        }
        if (!userId.equals(resume.getUserId())) {
            throw new BusinessException(ResultCode.ACCESS_DENIED, "无权访问该资源。");
        }

        List<SectionDTO> sections = resume.getSections();
        if (isContentTooShort(sections)) {
            throw new BusinessException(ResultCode.RESUME_CONTENT_TOO_SHORT, "简历内容过少，无法生成有效点评。");
        }

        ResumeReview review = new ResumeReview();
        review.setResumeId(resumeId);
        review.setUserId(userId);
        review.setJobDescription(request.getJobDescription());
        // model_name 留空，等异步任务真正调用 LLM 后由 AiResumeReviewService 填入；
        // 避免使用 "pending" 字符串污染模型字段语义（status 已表达 pending 状态）。
        review.setModelName(null);
        review.setStatus(BizConstant.TASK_STATUS_PENDING);
        review.setDeleted(BizConstant.NOT_DELETED);
        review.setCreatedAt(LocalDateTime.now());
        review.setUpdatedAt(LocalDateTime.now());
        resumeReviewMapper.insert(review);

        aiResumeReviewService.executeReview(review.getId(), resume, request.getJobDescription());

        ResumeReviewResponse response = new ResumeReviewResponse();
        response.setReviewId(review.getId());
        response.setResumeId(resumeId);
        response.setCreatedAt(review.getCreatedAt());
        return response;
    }

    /**
     * 获取最新点评。
     */
    public ResumeReviewResponse getLatestReview(String userId, String resumeId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null || BizConstant.DELETED.equals(resume.getDeleted())) {
            throw new BusinessException(ResultCode.RESUME_NOT_FOUND, "简历不存在。");
        }
        if (!userId.equals(resume.getUserId())) {
            throw new BusinessException(ResultCode.ACCESS_DENIED, "无权访问该资源。");
        }

        LambdaQueryWrapper<ResumeReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResumeReview::getResumeId, resumeId)
                .eq(ResumeReview::getStatus, BizConstant.REVIEW_STATUS_SUCCESS)
                .eq(ResumeReview::getDeleted, BizConstant.NOT_DELETED)
                .orderByDesc(ResumeReview::getCreatedAt)
                .last("LIMIT 1");
        ResumeReview review = resumeReviewMapper.selectOne(wrapper);
        if (review == null) {
            return null;
        }
        return toResponse(review);
    }

    private boolean isContentTooShort(List<SectionDTO> sections) {
        if (sections == null || sections.isEmpty()) {
            return true;
        }
        int total = 0;
        for (SectionDTO section : sections) {
            if (section.getData() != null) {
                total += section.getData().toString().length();
            }
        }
        return total < 20;
    }

    private ResumeReviewResponse toResponse(ResumeReview review) {
        ResumeReviewResponse response = new ResumeReviewResponse();
        response.setReviewId(review.getId());
        response.setResumeId(review.getResumeId());
        response.setOverallScore(review.getOverallScore());
        response.setDimensionScores(review.getDimensionScores());
        response.setSuggestions(toSuggestionList(review.getSuggestions()));
        response.setHighlights(review.getHighlights());
        response.setCreatedAt(review.getCreatedAt());
        return response;
    }

    private List<ResumeReviewResponse.SuggestionDTO> toSuggestionList(List<ResumeReviewSuggestion> suggestions) {
        if (suggestions == null) {
            return List.of();
        }
        return suggestions.stream().map(s -> {
            ResumeReviewResponse.SuggestionDTO dto = new ResumeReviewResponse.SuggestionDTO();
            dto.setSectionType(s.getSectionType());
            dto.setTitle(s.getTitle());
            dto.setProblem(s.getProblem());
            dto.setAdvice(s.getAdvice());
            dto.setPriority(s.getPriority());
            return dto;
        }).toList();
    }
}
