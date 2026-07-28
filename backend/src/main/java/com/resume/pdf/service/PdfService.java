package com.resume.pdf.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.microsoft.playwright.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * PDF 导出任务服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfService {

    @Value("${app.playwright.chromium-args:--no-sandbox,--disable-setuid-sandbox}")
    private String chromiumArgsString;

    private final PdfTaskMapper pdfTaskMapper;
    private final ResumeService resumeService;
    private final TemplateService templateService;
    private final ResumeRenderService resumeRenderService;
    private final MinioStorageService minioStorageService;
    private final ResumeSectionValidator resumeSectionValidator;

    /**
     * 创建 PDF 导出任务。
     * templateId 为可选字段：传入则仅覆盖本次导出使用的模板，不修改 resume.template_id。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> exportPdf(String userId, String resumeId, String templateId) {
        log.info("exportPdf start: userId={}, resumeId={}, templateId={}", userId, resumeId, templateId);
        Resume resume = resumeService.getResumeEntity(userId, resumeId);

        validateResumeForExport(resume);

        String exportTemplateId = StringUtils.isNotBlank(templateId) ? templateId : resume.getTemplateId();
        Template template = templateService.getTemplateEntity(exportTemplateId);

        PdfTask task = new PdfTask();
        task.setUserId(userId);
        task.setResumeId(resumeId);
        task.setTemplateId(exportTemplateId);
        task.setStatus(BizConstant.TASK_STATUS_PENDING);
        task.setDeleted(BizConstant.NOT_DELETED);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        pdfTaskMapper.insert(task);

        generatePdf(task, resume, template);

        resumeService.incrementExportCount(resumeId);

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("taskId", task.getId());
        result.put("status", task.getStatus());
        return result;
    }

    /**
     * 查询 PDF 任务。
     */
    public PdfTaskResponse getTask(String userId, String taskId) {
        PdfTask task = pdfTaskMapper.selectById(taskId);
        if (task == null || BizConstant.DELETED.equals(task.getDeleted())) {
            throw new BusinessException(ResultCode.PDF_TASK_NOT_FOUND, "PDF 任务不存在。");
        }
        if (!userId.equals(task.getUserId())) {
            throw new BusinessException(ResultCode.ACCESS_DENIED, "无权访问该资源。");
        }
        return toResponse(task);
    }

    /**
     * 下载 PDF 文件。
     */
    public byte[] downloadPdf(String userId, String taskId) {
        PdfTask task = pdfTaskMapper.selectById(taskId);
        if (task == null || BizConstant.DELETED.equals(task.getDeleted())) {
            throw new BusinessException(ResultCode.PDF_TASK_NOT_FOUND, "PDF 任务不存在。");
        }
        if (!userId.equals(task.getUserId())) {
            throw new BusinessException(ResultCode.ACCESS_DENIED, "无权访问该资源。");
        }
        if (!BizConstant.TASK_STATUS_SUCCESS.equals(task.getStatus())) {
            throw new BusinessException(ResultCode.PDF_FILE_NOT_READY, "PDF 文件尚未生成完成。");
        }
        if (StringUtils.isBlank(task.getFilePath())) {
            throw new BusinessException(ResultCode.PDF_EXPORT_FAILED, "PDF 文件路径不存在。");
        }

        try {
            return minioStorageService.download(minioStorageService.getBucketPdfs(), task.getFilePath());
        } catch (Exception e) {
            log.error("Download PDF from MinIO failed: taskId={}", taskId, e);
            throw new BusinessException(ResultCode.PDF_EXPORT_FAILED, "PDF 文件读取失败。");
        }
    }

    /**
     * 以流方式下载 PDF 文件，供 Controller 流式回写响应体，避免大文件 OOM。
     * <p>
     * 返回的 InputStream 由调用方负责关闭（Controller 中通过 try-with-resources 关闭）。
     * </p>
     */
    public InputStream downloadPdfStream(String userId, String taskId) {
        PdfTask task = pdfTaskMapper.selectById(taskId);
        if (task == null || BizConstant.DELETED.equals(task.getDeleted())) {
            throw new BusinessException(ResultCode.PDF_TASK_NOT_FOUND, "PDF 任务不存在。");
        }
        if (!userId.equals(task.getUserId())) {
            throw new BusinessException(ResultCode.ACCESS_DENIED, "无权访问该资源。");
        }
        if (!BizConstant.TASK_STATUS_SUCCESS.equals(task.getStatus())) {
            throw new BusinessException(ResultCode.PDF_FILE_NOT_READY, "PDF 文件尚未生成完成。");
        }
        if (StringUtils.isBlank(task.getFilePath())) {
            throw new BusinessException(ResultCode.PDF_EXPORT_FAILED, "PDF 文件路径不存在。");
        }
        return minioStorageService.downloadStream(minioStorageService.getBucketPdfs(), task.getFilePath());
    }

    /**
     * 清理指定简历关联的所有 PDF 任务及 MinIO 文件。
     */
    @Transactional(rollbackFor = Exception.class)
    public void cleanupTasksByResume(String userId, String resumeId) {
        LambdaQueryWrapper<PdfTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PdfTask::getUserId, userId)
                .eq(PdfTask::getResumeId, resumeId);
        List<PdfTask> tasks = pdfTaskMapper.selectList(wrapper);
        for (PdfTask task : tasks) {
            if (StringUtils.isNotBlank(task.getFilePath())) {
                minioStorageService.remove(minioStorageService.getBucketPdfs(), task.getFilePath());
            }
        }
        if (!tasks.isEmpty()) {
            pdfTaskMapper.delete(wrapper);
        }
    }

    private void generatePdf(PdfTask task, Resume resume, Template template) {
        Path tempDir = null;
        try {
            task.setStatus(BizConstant.TASK_STATUS_PROCESSING);
            pdfTaskMapper.updateById(task);
            log.info("generatePdf -> processing: taskId={}, resumeId={}", task.getId(), resume.getId());

            String html = resumeRenderService.render(resume, template);
            String fileName = buildFileName(resume);
            tempDir = Files.createTempDirectory("resume-pdf-");
            Path htmlPath = tempDir.resolve("resume.html");
            Path pdfPath = tempDir.resolve(fileName);
            Files.writeString(htmlPath, html);

            try (Playwright playwright = Playwright.create();
                 Browser browser = playwright.chromium().launch(buildLaunchOptions());
                 BrowserContext context = browser.newContext();
                 Page page = context.newPage()) {
                page.navigate(htmlPath.toUri().toString());
                page.pdf(new Page.PdfOptions().setPath(pdfPath)
                        .setFormat("A4")
                        .setPrintBackground(true));
            }

            String objectName = task.getUserId() + "/pdfs/" + task.getId() + "/" + fileName;
            minioStorageService.upload(minioStorageService.getBucketPdfs(), objectName,
                    new FileInputStream(pdfPath.toFile()), pdfPath.toFile().length(), "application/pdf");

            task.setFilePath(objectName);
            task.setFileName(fileName);
            task.setFileSize(pdfPath.toFile().length());
            task.setStatus(BizConstant.TASK_STATUS_SUCCESS);
            task.setCompletedAt(LocalDateTime.now());
            log.info("generatePdf success: taskId={}, fileSize={}, fileName={}",
                    task.getId(), task.getFileSize(), task.getFileName());
        } catch (Exception e) {
            log.error("Generate PDF failed: taskId={}", task.getId(), e);
            task.setStatus(BizConstant.TASK_STATUS_FAILED);
            String msg = e.getMessage() == null ? "" : e.getMessage();
            task.setErrorMsg(StringUtils.abbreviate("PDF 生成失败：" + msg, 500));
        } finally {
            if (tempDir != null) {
                try {
                    try (var files = java.nio.file.Files.walk(tempDir)) {
                        files.sorted(java.util.Comparator.reverseOrder())
                             .map(Path::toFile)
                             .forEach(f -> { if (!f.delete()) f.deleteOnExit(); });
                    }
                } catch (Exception e) {
                    log.warn("Failed to clean temp dir: {}", tempDir, e);
                }
            }
            task.setUpdatedAt(LocalDateTime.now());
            pdfTaskMapper.updateById(task);
        }
    }

    private void validateResumeForExport(Resume resume) {
        resumeSectionValidator.validateForExport(resume.getSections());
    }

    private String buildFileName(Resume resume) {
        List<SectionDTO> sections = resume.getSections() != null ? resume.getSections() : List.of();
        String name = "";
        String targetPosition = resume.getTargetPosition();

        SectionDTO profileSection = sections.stream()
                .filter(s -> BizConstant.SECTION_TYPE_PROFILE.equals(s.getType()))
                .findFirst()
                .orElse(null);
        if (profileSection != null && profileSection.getData() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> profile = (Map<String, Object>) profileSection.getData();
            name = getString(profile, "name");
        }

        if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(targetPosition)) {
            return name + "_" + targetPosition + "_简历.pdf";
        }
        if (StringUtils.isNotBlank(name)) {
            return name + "_简历.pdf";
        }
        return "我的简历_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";
    }

    private BrowserType.LaunchOptions buildLaunchOptions() {
        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions().setArgs(getChromiumArgs());
        String executablePath = System.getenv("PLAYWRIGHT_CHROMIUM_EXECUTABLE");
        if (StringUtils.isNotBlank(executablePath)) {
            log.info("Using custom Chromium executable for Playwright: {}", executablePath);
            options.setExecutablePath(Path.of(executablePath.trim()));
        }
        return options;
    }

    private List<String> getChromiumArgs() {
        String args = StringUtils.isNotBlank(chromiumArgsString)
                ? chromiumArgsString
                : "--no-sandbox,--disable-setuid-sandbox";
        return Arrays.stream(args.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .toList();
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }

    private PdfTaskResponse toResponse(PdfTask task) {
        PdfTaskResponse response = new PdfTaskResponse();
        response.setTaskId(task.getId());
        response.setResumeId(task.getResumeId());
        response.setTemplateId(task.getTemplateId());
        response.setStatus(task.getStatus());
        response.setFileName(task.getFileName());
        response.setFileSize(task.getFileSize());
        response.setErrorMsg(task.getErrorMsg());
        response.setCreatedAt(task.getCreatedAt());
        response.setCompletedAt(task.getCompletedAt());
        return response;
    }
}
