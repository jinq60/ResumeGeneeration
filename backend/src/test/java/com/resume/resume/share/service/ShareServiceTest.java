package com.resume.resume.share.service;

import com.resume.common.constant.BizConstant;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.common.service.ResumeRenderService;
import com.resume.resume.entity.Resume;
import com.resume.resume.mapper.ResumeMapper;
import com.resume.resume.share.dto.ShareResponse;
import com.resume.resume.share.entity.ResumeShare;
import com.resume.resume.share.mapper.ResumeShareMapper;
import com.resume.template.entity.Template;
import com.resume.template.service.TemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ShareServiceTest {

    @Mock
    private ResumeShareMapper resumeShareMapper;

    @Mock
    private ResumeMapper resumeMapper;

    @Mock
    private ResumeRenderService resumeRenderService;

    @Mock
    private TemplateService templateService;

    private ShareService shareService;

    @BeforeEach
    void setUp() {
        shareService = new ShareService(resumeShareMapper, resumeMapper, resumeRenderService, templateService);
    }

    private Resume buildResume(String userId, String resumeId) {
        Resume resume = new Resume();
        resume.setId(resumeId);
        resume.setUserId(userId);
        resume.setTitle("我的简历");
        resume.setTemplateId("template_classic_single");
        resume.setDeleted(BizConstant.NOT_DELETED);
        return resume;
    }

    @Test
    void createShare_shouldReturnShareResponse() {
        when(resumeMapper.selectById("resume_1")).thenReturn(buildResume("user_1", "resume_1"));
        when(resumeShareMapper.insert(any(ResumeShare.class))).thenAnswer(inv -> {
            ((ResumeShare) inv.getArgument(0)).setId("share_1");
            return 1;
        });

        ShareResponse response = shareService.createShare("user_1", "resume_1", false, null);

        assertEquals(ShareService.SHARE_STATUS_ACTIVE, response.getStatus());
        assertNotNull(response.getToken());
        assertEquals("/share/" + response.getToken(), response.getUrl());
    }

    @Test
    void createShare_shouldRejectForeignResume() {
        when(resumeMapper.selectById("resume_1")).thenReturn(buildResume("user_2", "resume_1"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> shareService.createShare("user_1", "resume_1", false, null));
        assertEquals(ResultCode.ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    void createShare_shouldRejectMissingResume() {
        when(resumeMapper.selectById("resume_missing")).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> shareService.createShare("user_1", "resume_missing", false, null));
        assertEquals(ResultCode.RESUME_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void createShare_shouldPersistHideContactAndExpiresAt() {
        when(resumeMapper.selectById("resume_1")).thenReturn(buildResume("user_1", "resume_1"));
        when(resumeShareMapper.insert(any(ResumeShare.class))).thenAnswer(inv -> {
            ((ResumeShare) inv.getArgument(0)).setId("share_1");
            return 1;
        });
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

        ShareResponse response = shareService.createShare("user_1", "resume_1", true, expiresAt);

        assertTrue(response.getHideContact());
        assertEquals(expiresAt, response.getExpiresAt());
    }

    @Test
    void createShare_shouldRejectPastExpiresAt() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> shareService.createShare("user_1", "resume_1", false,
                        LocalDateTime.now().minusMinutes(1)));
        assertEquals(ResultCode.PARAM_INVALID, ex.getErrorCode());
    }

    @Test
    void renderSharePage_shouldReturnWrappedHtml() {
        ResumeShare share = new ResumeShare();
        share.setResumeId("resume_1");
        share.setStatus(ShareService.SHARE_STATUS_ACTIVE);
        share.setDeleted(BizConstant.NOT_DELETED);
        when(resumeShareMapper.selectOne(any())).thenReturn(share);
        when(resumeMapper.selectById("resume_1")).thenReturn(buildResume("user_1", "resume_1"));
        Template template = new Template();
        template.setId("template_classic_single");
        when(templateService.getTemplateEntityForRender("template_classic_single")).thenReturn(template);
        when(resumeRenderService.render(any(), any())).thenReturn("<div>简历内容</div>");

        String html = shareService.renderSharePage("some-token");

        assertTrue(html.contains("简历内容"));
        assertTrue(html.contains("noindex"));
        assertTrue(html.contains("智能简历"));
    }

    @Test
    void renderSharePage_hideContact_shouldUseHideContactRenderOptions() {
        ResumeShare share = new ResumeShare();
        share.setResumeId("resume_1");
        share.setStatus(ShareService.SHARE_STATUS_ACTIVE);
        share.setHideContact(true);
        share.setDeleted(BizConstant.NOT_DELETED);
        when(resumeShareMapper.selectOne(any())).thenReturn(share);
        when(resumeMapper.selectById("resume_1")).thenReturn(buildResume("user_1", "resume_1"));
        Template template = new Template();
        template.setId("template_classic_single");
        when(templateService.getTemplateEntityForRender("template_classic_single")).thenReturn(template);
        when(resumeRenderService.render(any(), any(),
                eq(ResumeRenderService.RenderOptions.withHiddenContact()))).thenReturn("<div>无联系方式</div>");

        String html = shareService.renderSharePage("token");

        assertTrue(html.contains("无联系方式"));
    }

    @Test
    void renderSharePage_withoutHideContact_shouldUseDefaultRender() {
        ResumeShare share = new ResumeShare();
        share.setResumeId("resume_1");
        share.setStatus(ShareService.SHARE_STATUS_ACTIVE);
        share.setHideContact(false);
        share.setDeleted(BizConstant.NOT_DELETED);
        when(resumeShareMapper.selectOne(any())).thenReturn(share);
        when(resumeMapper.selectById("resume_1")).thenReturn(buildResume("user_1", "resume_1"));
        Template template = new Template();
        template.setId("template_classic_single");
        when(templateService.getTemplateEntityForRender("template_classic_single")).thenReturn(template);
        when(resumeRenderService.render(any(), any())).thenReturn("<div>含联系方式</div>");

        String html = shareService.renderSharePage("token");

        assertTrue(html.contains("含联系方式"));
    }

    @Test
    void renderSharePage_shouldRejectInvalidToken() {
        when(resumeShareMapper.selectOne(any())).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> shareService.renderSharePage("invalid-token"));
        assertEquals(ResultCode.RESOURCE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void renderSharePage_shouldRejectExpired() {
        ResumeShare share = new ResumeShare();
        share.setResumeId("resume_1");
        share.setStatus(ShareService.SHARE_STATUS_ACTIVE);
        share.setDeleted(BizConstant.NOT_DELETED);
        share.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(resumeShareMapper.selectOne(any())).thenReturn(share);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> shareService.renderSharePage("expired-token"));
        assertEquals(ResultCode.RESOURCE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void renderSharePage_shouldRejectWhenResumeDeleted() {
        ResumeShare share = new ResumeShare();
        share.setResumeId("resume_1");
        share.setStatus(ShareService.SHARE_STATUS_ACTIVE);
        share.setDeleted(BizConstant.NOT_DELETED);
        when(resumeShareMapper.selectOne(any())).thenReturn(share);
        Resume deleted = buildResume("user_1", "resume_1");
        deleted.setDeleted(BizConstant.DELETED);
        when(resumeMapper.selectById("resume_1")).thenReturn(deleted);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> shareService.renderSharePage("token"));
        assertEquals(ResultCode.RESOURCE_NOT_FOUND, ex.getErrorCode());
    }
}
