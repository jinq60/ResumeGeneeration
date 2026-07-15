package com.resume.resume.service;

import com.resume.ai.service.AiResumeReviewService;
import com.resume.common.constant.BizConstant;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.resume.dto.ResumeReviewResponse;
import com.resume.resume.dto.ReviewResumeRequest;
import com.resume.resume.dto.SectionDTO;
import com.resume.resume.entity.Resume;
import com.resume.resume.entity.ResumeReview;
import com.resume.resume.mapper.ResumeMapper;
import com.resume.resume.mapper.ResumeReviewMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeReviewServiceTest {

    @Mock
    private ResumeReviewMapper resumeReviewMapper;

    @Mock
    private ResumeMapper resumeMapper;

    @Mock
    private AiResumeReviewService aiResumeReviewService;

    private ResumeReviewService resumeReviewService;

    private final String userId = "user_1";
    private final String resumeId = "resume_1";

    @BeforeEach
    void setUp() {
        resumeReviewService = new ResumeReviewService(
                resumeReviewMapper, resumeMapper, aiResumeReviewService);
    }

    @Test
    void reviewResume_shouldCreatePendingReviewAndTriggerAsync() {
        Resume resume = buildResume(userId, resumeId);
        resume.setSections(buildRichSections());
        when(resumeMapper.selectById(resumeId)).thenReturn(resume);
        when(resumeReviewMapper.insert(any(ResumeReview.class))).thenAnswer(inv -> {
            ResumeReview r = inv.getArgument(0);
            r.setId("review_1");
            return 1;
        });

        ReviewResumeRequest request = new ReviewResumeRequest();
        request.setJobDescription("Java工程师");

        ResumeReviewResponse response = resumeReviewService.reviewResume(userId, resumeId, request);

        assertNotNull(response.getReviewId());
        assertEquals("review_1", response.getReviewId());
        assertEquals(resumeId, response.getResumeId());
        assertNotNull(response.getCreatedAt());

        ArgumentCaptor<ResumeReview> captor = ArgumentCaptor.forClass(ResumeReview.class);
        verify(resumeReviewMapper).insert(captor.capture());
        ResumeReview inserted = captor.getValue();
        assertEquals(resumeId, inserted.getResumeId());
        assertEquals(userId, inserted.getUserId());
        assertEquals(BizConstant.TASK_STATUS_PENDING, inserted.getStatus());

        verify(aiResumeReviewService).executeReview(eq(inserted.getId()), eq(resume), eq("Java工程师"));
    }

    @Test
    void reviewResume_shouldThrowWhenResumeNotFound() {
        when(resumeMapper.selectById(resumeId)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> resumeReviewService.reviewResume(userId, resumeId, new ReviewResumeRequest()));
        assertEquals(ResultCode.RESUME_NOT_FOUND, ex.getErrorCode());
        verify(resumeReviewMapper, never()).insert(any());
    }

    @Test
    void reviewResume_shouldThrowWhenAccessDenied() {
        Resume resume = buildResume("other_user", resumeId);
        when(resumeMapper.selectById(resumeId)).thenReturn(resume);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> resumeReviewService.reviewResume(userId, resumeId, new ReviewResumeRequest()));
        assertEquals(ResultCode.ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    void reviewResume_shouldThrowWhenContentTooShort() {
        Resume resume = buildResume(userId, resumeId);
        resume.setSections(List.of());
        when(resumeMapper.selectById(resumeId)).thenReturn(resume);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> resumeReviewService.reviewResume(userId, resumeId, new ReviewResumeRequest()));
        assertEquals(ResultCode.RESUME_CONTENT_TOO_SHORT, ex.getErrorCode());
    }

    @Test
    void getLatestReview_shouldReturnReviewWhenFound() {
        Resume resume = buildResume(userId, resumeId);
        when(resumeMapper.selectById(resumeId)).thenReturn(resume);

        ResumeReview review = new ResumeReview();
        review.setId("review_1");
        review.setResumeId(resumeId);
        review.setOverallScore(85);
        review.setDimensionScores(Map.of("completeness", 85));
        review.setSuggestions(List.of());
        review.setHighlights(List.of("亮点1"));
        review.setCreatedAt(java.time.LocalDateTime.now());
        when(resumeReviewMapper.selectOne(any())).thenReturn(review);

        ResumeReviewResponse response = resumeReviewService.getLatestReview(userId, resumeId);

        assertNotNull(response);
        assertEquals("review_1", response.getReviewId());
        assertEquals(85, response.getOverallScore());
    }

    @Test
    void getLatestReview_shouldReturnNullWhenNoReview() {
        Resume resume = buildResume(userId, resumeId);
        when(resumeMapper.selectById(resumeId)).thenReturn(resume);
        when(resumeReviewMapper.selectOne(any())).thenReturn(null);

        ResumeReviewResponse response = resumeReviewService.getLatestReview(userId, resumeId);

        assertNull(response);
    }

    @Test
    void getLatestReview_shouldThrowWhenAccessDenied() {
        Resume resume = buildResume("other_user", resumeId);
        when(resumeMapper.selectById(resumeId)).thenReturn(resume);

        assertThrows(BusinessException.class,
                () -> resumeReviewService.getLatestReview(userId, resumeId));
    }

    private Resume buildResume(String ownerId, String id) {
        Resume resume = new Resume();
        resume.setId(id);
        resume.setUserId(ownerId);
        resume.setTitle("测试简历");
        resume.setDeleted(BizConstant.NOT_DELETED);
        resume.setSections(List.of());
        return resume;
    }

    private List<SectionDTO> buildRichSections() {
        SectionDTO section = new SectionDTO();
        section.setId("s1");
        section.setType("profile");
        section.setTitle("个人信息");
        section.setOrder(1);
        section.setVisible(true);
        section.setData(Map.of("name", "张三", "phone", "13800138000", "email", "test@example.com"));
        return List.of(section);
    }
}
