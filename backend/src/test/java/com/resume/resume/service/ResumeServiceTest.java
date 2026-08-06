package com.resume.resume.service;

import com.resume.avatar.service.AvatarService;
import com.resume.pdf.service.PdfService;
import com.resume.resume.dto.DuplicateResumeResponse;
import com.resume.resume.dto.RenderSettings;
import com.resume.resume.dto.SectionDTO;
import com.resume.resume.dto.UpdateResumeRequest;
import com.resume.resume.entity.Resume;
import com.resume.resume.mapper.ResumeMapper;
import com.resume.template.service.TemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ResumeServiceTest {

    @Mock
    private ResumeMapper resumeMapper;

    @Mock
    private TemplateService templateService;

    @Mock
    private PdfService pdfService;

    @Mock
    private AvatarService avatarService;

    @Mock
    private com.resume.common.service.AuditLogService auditLogService;

    @Mock
    private com.resume.resume.share.service.ShareService shareService;

    private ResumeSectionValidator resumeSectionValidator;

    private ResumeService resumeService;

    @BeforeEach
    void setUp() {
        resumeSectionValidator = new ResumeSectionValidator();
        resumeService = new ResumeService(resumeMapper, templateService,
                pdfService, avatarService, resumeSectionValidator, auditLogService, shareService);
    }

    @Test
    void updateResume_shouldAllowDefaultSectionsDraft() {
        String resumeId = "resume_1";
        String userId = "user_1";

        Resume existing = new Resume();
        existing.setId(resumeId);
        existing.setUserId(userId);
        existing.setTitle("我的简历");
        existing.setTemplateId("template_classic_single");
        existing.setSections(List.of(
                createSection("profile", "个人信息", 0, new HashMap<String, Object>()),
                createSection("education", "教育经历", 1, new java.util.ArrayList<>()),
                createSection("project", "项目经历", 2, new java.util.ArrayList<>()),
                createSection("work", "工作经历", 3, new java.util.ArrayList<>()),
                createSection("skill", "技能 & 技术栈", 4, new java.util.ArrayList<>()),
                createSection("introduction", "自我介绍", 5, new HashMap<String, Object>())
        ));

        when(resumeMapper.selectById(resumeId)).thenReturn(existing);

        UpdateResumeRequest request = new UpdateResumeRequest();
        request.setTitle(existing.getTitle());
        request.setTargetPosition("");
        request.setSections(existing.getSections());

        assertDoesNotThrow(() -> resumeService.updateResume(userId, resumeId, request));
        verify(resumeMapper).updateById(any(Resume.class));
    }

    private SectionDTO createSection(String type, String title, int order, Object data) {
        SectionDTO section = new SectionDTO();
        section.setId("sec_" + order);
        section.setType(type);
        section.setTitle(title);
        section.setOrder(order);
        section.setVisible(true);
        section.setData(data);
        return section;
    }

    @Test
    void updateResume_shouldAllowEmptyProfileDraft() {
        String resumeId = "resume_1";
        String userId = "user_1";

        Resume existing = new Resume();
        existing.setId(resumeId);
        existing.setUserId(userId);
        existing.setTitle("我的简历");
        existing.setTemplateId("template_1");
        existing.setSections(List.of());

        when(resumeMapper.selectById(resumeId)).thenReturn(existing);

        UpdateResumeRequest request = new UpdateResumeRequest();
        SectionDTO profile = new SectionDTO();
        profile.setId("sec_profile");
        profile.setType("profile");
        profile.setTitle("个人信息");
        profile.setOrder(0);
        profile.setVisible(true);
        profile.setData(new HashMap<String, Object>());
        request.setSections(List.of(profile));

        assertDoesNotThrow(() -> resumeService.updateResume(userId, resumeId, request));
        verify(resumeMapper).updateById(any(Resume.class));
    }

    @Test
    void updateResume_shouldPersistRenderSettings() {
        String resumeId = "resume_1";
        String userId = "user_1";

        Resume existing = new Resume();
        existing.setId(resumeId);
        existing.setUserId(userId);
        existing.setTitle("我的简历");
        existing.setTemplateId("template_1");
        existing.setSections(List.of());
        when(resumeMapper.selectById(resumeId)).thenReturn(existing);

        UpdateResumeRequest request = new UpdateResumeRequest();
        request.setRenderSettings(new RenderSettings()
                .setAutoOnePage(true)
                .setBaseFontSize(9.5)
                .setAccentColor("#2f6fed"));

        resumeService.updateResume(userId, resumeId, request);

        ArgumentCaptor<Resume> captor = ArgumentCaptor.forClass(Resume.class);
        verify(resumeMapper).updateById(captor.capture());
        assertEquals(true, captor.getValue().getRenderSettings().getAutoOnePage());
        assertEquals(9.5, captor.getValue().getRenderSettings().getBaseFontSize());
        assertEquals("#2f6fed", captor.getValue().getRenderSettings().getAccentColor());
    }

    @Test
    void duplicateResume_shouldTruncateTitleWhenTooLong() {
        String resumeId = "resume_1";
        String userId = "user_1";
        String longTitle = "a".repeat(126);

        Resume source = new Resume();
        source.setId(resumeId);
        source.setUserId(userId);
        source.setTitle(longTitle);
        source.setScene("social_recruitment");
        source.setTemplateId("template_1");
        source.setSections(List.of());

        when(resumeMapper.selectById(resumeId)).thenReturn(source);
        when(resumeMapper.insert(any(Resume.class))).thenAnswer(inv -> {
            Resume r = inv.getArgument(0);
            r.setId("resume_copy_1");
            return 1;
        });

        DuplicateResumeResponse result = resumeService.duplicateResume(userId, resumeId);

        String copiedTitle = result.getTitle();
        assertEquals(128, copiedTitle.length());
        assertTrue(copiedTitle.endsWith(" 副本"));
    }

    @Test
    void deleteResume_shouldCleanupAssociatedTasksAndFiles() {
        String resumeId = "resume_1";
        String userId = "user_1";

        Resume existing = new Resume();
        existing.setId(resumeId);
        existing.setUserId(userId);
        existing.setTitle("我的简历");
        existing.setTemplateId("template_1");
        existing.setSections(List.of());
        existing.setDeleted(0);

        when(resumeMapper.selectById(resumeId)).thenReturn(existing);

        resumeService.deleteResume(userId, resumeId);

        verify(pdfService).cleanupTasksByResume(userId, resumeId);
        verify(avatarService).cleanupTasksByResume(userId, resumeId);
        verify(resumeMapper).deleteById(resumeId);
    }
}
