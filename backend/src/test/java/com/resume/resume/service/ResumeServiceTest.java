package com.resume.resume.service;

import com.resume.avatar.entity.AvatarTask;
import com.resume.avatar.mapper.AvatarTaskMapper;
import com.resume.common.service.MinioStorageService;
import com.resume.pdf.entity.PdfTask;
import com.resume.pdf.mapper.PdfTaskMapper;
import com.resume.resume.dto.SectionDTO;
import com.resume.resume.dto.UpdateResumeRequest;
import com.resume.resume.entity.Resume;
import com.resume.resume.mapper.ResumeMapper;
import com.resume.template.service.TemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private PdfTaskMapper pdfTaskMapper;

    @Mock
    private AvatarTaskMapper avatarTaskMapper;

    @Mock
    private MinioStorageService minioStorageService;

    private ResumeSectionValidator resumeSectionValidator;

    private ResumeService resumeService;

    @BeforeEach
    void setUp() {
        resumeSectionValidator = new ResumeSectionValidator();
        resumeService = new ResumeService(resumeMapper, templateService,
                pdfTaskMapper, avatarTaskMapper, minioStorageService, resumeSectionValidator);
        when(minioStorageService.getBucketPdfs()).thenReturn("pdfs");
        when(minioStorageService.getBucketAvatars()).thenReturn("avatars");
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

        Map<String, Object> result = resumeService.duplicateResume(userId, resumeId);

        String copiedTitle = (String) result.get("title");
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

        PdfTask pdfTask = new PdfTask();
        pdfTask.setId("pdf_1");
        pdfTask.setFilePath("user_1/pdfs/pdf_1/简历.pdf");
        when(pdfTaskMapper.selectList(any())).thenReturn(List.of(pdfTask));

        AvatarTask avatarTask = new AvatarTask();
        avatarTask.setId("avatar_1");
        avatarTask.setSourceImageUrl("/uploads/avatars/user_1/avatars/avatar_1_source.png");
        when(avatarTaskMapper.selectList(any())).thenReturn(List.of(avatarTask));

        resumeService.deleteResume(userId, resumeId);

        verify(minioStorageService).remove("pdfs", "user_1/pdfs/pdf_1/简历.pdf");
        verify(minioStorageService).remove("avatars", "user_1/avatars/avatar_1_source.png");
        verify(pdfTaskMapper).update(any(PdfTask.class), any());
        verify(avatarTaskMapper).update(any(AvatarTask.class), any());
        assertEquals(1, existing.getDeleted());
    }
}
