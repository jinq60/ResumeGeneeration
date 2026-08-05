package com.resume.pdf.service;

import com.resume.common.constant.BizConstant;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.common.service.MinioStorageService;
import com.resume.common.service.ResumeRenderService;
import com.resume.pdf.dto.PdfTaskResponse;
import com.resume.pdf.entity.PdfTask;
import com.resume.pdf.mapper.PdfTaskMapper;
import com.resume.resume.dto.SectionDTO;
import com.resume.resume.entity.Resume;
import com.resume.resume.service.ResumeSectionValidator;
import com.resume.resume.service.ResumeService;
import com.resume.template.entity.Template;
import com.resume.template.service.TemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PdfServiceTest {

    @Mock
    private PdfTaskMapper pdfTaskMapper;

    @Mock
    private ResumeService resumeService;

    @Mock
    private TemplateService templateService;

    @Mock
    private ResumeRenderService resumeRenderService;

    @Mock
    private MinioStorageService minioStorageService;

    private ResumeSectionValidator resumeSectionValidator;

    private PdfService pdfService;

    @BeforeEach
    void setUp() {
        resumeSectionValidator = new ResumeSectionValidator();
        Executor executor = (command) -> command.run();
        pdfService = new PdfService(pdfTaskMapper, resumeService, templateService,
                resumeRenderService, minioStorageService, resumeSectionValidator, executor);
        when(minioStorageService.getBucketPdfs()).thenReturn("pdfs");
    }

    @Test
    void getTask_shouldReturnResponseWhenOwner() {
        PdfTask task = new PdfTask();
        task.setId("task_1");
        task.setUserId("user_1");
        task.setStatus(BizConstant.TASK_STATUS_SUCCESS);
        task.setFileName("张三_简历.pdf");
        task.setFileSize(1024L);

        when(pdfTaskMapper.selectById("task_1")).thenReturn(task);

        PdfTaskResponse response = pdfService.getTask("user_1", "task_1");

        assertEquals("task_1", response.getTaskId());
        assertEquals(BizConstant.TASK_STATUS_SUCCESS, response.getStatus());
        assertEquals("张三_简历.pdf", response.getFileName());
        assertEquals(1024L, response.getFileSize());
    }

    @Test
    void getTask_shouldRejectNotFound() {
        when(pdfTaskMapper.selectById("task_missing")).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pdfService.getTask("user_1", "task_missing"));
        assertEquals(ResultCode.PDF_TASK_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void getTask_shouldRejectAccessDenied() {
        PdfTask task = new PdfTask();
        task.setId("task_1");
        task.setUserId("other_user");

        when(pdfTaskMapper.selectById("task_1")).thenReturn(task);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pdfService.getTask("user_1", "task_1"));
        assertEquals(ResultCode.ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    void downloadPdf_shouldReturnBytesWhenSuccess() {
        PdfTask task = new PdfTask();
        task.setId("task_1");
        task.setUserId("user_1");
        task.setStatus(BizConstant.TASK_STATUS_SUCCESS);
        task.setFilePath("user_1/pdfs/task_1/张三_简历.pdf");

        when(pdfTaskMapper.selectById("task_1")).thenReturn(task);
        when(minioStorageService.download("pdfs", "user_1/pdfs/task_1/张三_简历.pdf"))
                .thenReturn(new byte[]{1, 2, 3});

        byte[] content = pdfService.downloadPdf("user_1", "task_1");

        assertArrayEquals(new byte[]{1, 2, 3}, content);
        verify(minioStorageService).download("pdfs", "user_1/pdfs/task_1/张三_简历.pdf");
    }

    @Test
    void downloadPdf_shouldRejectWhenNotReady() {
        PdfTask task = new PdfTask();
        task.setId("task_1");
        task.setUserId("user_1");
        task.setStatus(BizConstant.TASK_STATUS_PENDING);

        when(pdfTaskMapper.selectById("task_1")).thenReturn(task);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pdfService.downloadPdf("user_1", "task_1"));
        assertEquals(ResultCode.PDF_FILE_NOT_READY, ex.getErrorCode());
    }

    @Test
    void downloadPdf_shouldRejectWhenFilePathBlank() {
        PdfTask task = new PdfTask();
        task.setId("task_1");
        task.setUserId("user_1");
        task.setStatus(BizConstant.TASK_STATUS_SUCCESS);
        task.setFilePath("");

        when(pdfTaskMapper.selectById("task_1")).thenReturn(task);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pdfService.downloadPdf("user_1", "task_1"));
        assertEquals(ResultCode.PDF_EXPORT_FAILED, ex.getErrorCode());
    }

    @Test
    void exportPdf_shouldRejectWhenResumeNotFound() {
        when(resumeService.getResumeEntity(eq("user_1"), eq("resume_missing")))
                .thenThrow(new BusinessException(ResultCode.RESUME_NOT_FOUND, "简历不存在。"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pdfService.exportPdf("user_1", "resume_missing", null));
        assertEquals(ResultCode.RESUME_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void exportPdf_shouldRejectWhenAccessDenied() {
        when(resumeService.getResumeEntity(eq("user_1"), eq("resume_1")))
                .thenThrow(new BusinessException(ResultCode.ACCESS_DENIED, "无权访问该资源。"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pdfService.exportPdf("user_1", "resume_1", null));
        assertEquals(ResultCode.ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    void exportPdf_shouldRejectWhenNameMissing() {
        Resume resume = buildResume("user_1", profileSection("", "13800000000", ""));
        when(resumeService.getResumeEntity("user_1", "resume_1")).thenReturn(resume);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pdfService.exportPdf("user_1", "resume_1", null));
        assertEquals(ResultCode.PDF_EXPORT_NAME_REQUIRED, ex.getErrorCode());
    }

    @Test
    void exportPdf_shouldRejectWhenContactMissing() {
        Resume resume = buildResume("user_1", profileSection("张三", "", ""));
        when(resumeService.getResumeEntity("user_1", "resume_1")).thenReturn(resume);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pdfService.exportPdf("user_1", "resume_1", null));
        assertEquals(ResultCode.PDF_EXPORT_CONTACT_REQUIRED, ex.getErrorCode());
    }

    @Test
    void exportPdf_shouldRejectWhenTemplateInactive() {
        Resume resume = buildResume("user_1", profileSection("张三", "13800000000", ""));
        when(resumeService.getResumeEntity("user_1", "resume_1")).thenReturn(resume);
        when(templateService.getTemplateEntity("tpl_1"))
                .thenThrow(new BusinessException(ResultCode.TEMPLATE_NOT_FOUND, "模板不存在。"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pdfService.exportPdf("user_1", "resume_1", "tpl_1"));
        assertEquals(ResultCode.TEMPLATE_NOT_FOUND, ex.getErrorCode());
    }

    private Resume buildResume(String userId, List<SectionDTO> sections) {
        Resume resume = new Resume();
        resume.setId("resume_1");
        resume.setUserId(userId);
        resume.setTemplateId("tpl_1");
        resume.setSections(sections);
        resume.setDeleted(BizConstant.NOT_DELETED);
        return resume;
    }

    private List<SectionDTO> profileSection(String name, String phone, String email) {
        SectionDTO section = new SectionDTO();
        section.setId("p1");
        section.setType("profile");
        section.setTitle("个人信息");
        section.setOrder(0);
        section.setVisible(true);
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("phone", phone);
        data.put("email", email);
        section.setData(data);
        return List.of(section);
    }
}
