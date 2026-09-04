package com.resume.pdf.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.resume.common.constant.BizConstant;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.common.service.AuditLogService;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * PDF 导出任务服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfService {

    @Value("${app.playwright.chromium-args:--no-sandbox,--disable-setuid-sandbox}")
    private String chromiumArgsString;

    /** 同时执行的 Playwright 导出上限，防止 Chromium 进程耗尽服务器内存。 */
    private static final int MAX_CONCURRENT_EXPORTS = 4;
    /** 排队等待导出信号量的最长时间。 */
    private static final long SEMAPHORE_WAIT_SECONDS = 30;

    private final PdfTaskMapper pdfTaskMapper;
    private final ResumeService resumeService;
    private final TemplateService templateService;
    private final ResumeRenderService resumeRenderService;
    private final MinioStorageService minioStorageService;
    private final ResumeSectionValidator resumeSectionValidator;
    private final AuditLogService auditLogService;
    private final com.resume.notification.service.NotificationService notificationService;
    @Qualifier("pdfTaskExecutor")
    private final Executor pdfTaskExecutor;

    private final Semaphore exportSemaphore = new Semaphore(MAX_CONCURRENT_EXPORTS);
    private final java.util.concurrent.ConcurrentHashMap<String, Object> exportLocks = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Playwright 与 Chromium 进程复用：每次导出不再启动/销毁浏览器，
     * 显著降低内存抖动与启动开销。信号量已限制并发导出数。
     */
    private final Object browserLock = new Object();
    private volatile Playwright sharedPlaywright;
    private volatile Browser sharedBrowser;

    /**
     * 创建 PDF 导出任务。
     * <p>
     * 仅创建 pending 任务并提交到专用线程池，立即返回；
     * 实际渲染在后台执行，前端通过 GET /pdf/tasks/{taskId} 轮询结果。
     * </p>
     * templateId 为可选字段：传入则仅覆盖本次导出使用的模板，不修改 resume.template_id。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> exportPdf(String userId, String resumeId, String templateId) {
        log.info("exportPdf start: userId={}, resumeId={}, templateId={}", userId, resumeId, templateId);
        Resume resume = resumeService.getResumeEntity(userId, resumeId);

        validateResumeForExport(resume);

        String lockKey = userId + ":" + resumeId;
        Object lock = exportLocks.computeIfAbsent(lockKey, k -> new Object());
        try {
            synchronized (lock) {
                // 去重：同 (userId, resumeId) 已有进行中的任务时直接复用，避免重复提交产生冗余导出；加锁防并发双插
                PdfTask existing = findActiveTask(userId, resumeId);
                if (existing != null) {
                    log.info("exportPdf deduplicated: reuse running task={}, userId={}, resumeId={}",
                            existing.getId(), userId, resumeId);
                    Map<String, Object> reused = new java.util.HashMap<>();
                    reused.put("taskId", existing.getId());
                    reused.put("status", existing.getStatus());
                    return reused;
                }

                String exportTemplateId = StringUtils.isNotBlank(templateId) ? templateId : resume.getTemplateId();
                // 渲染场景容忍 inactive/deleted 模板，历史简历仍可导出
                Template template = templateService.getTemplateEntityForRender(exportTemplateId);

                PdfTask task = new PdfTask();
                task.setUserId(userId);
                task.setResumeId(resumeId);
                task.setTemplateId(exportTemplateId);
                task.setStatus(BizConstant.TASK_STATUS_PENDING);
                task.setDeleted(BizConstant.NOT_DELETED);
                task.setCreatedAt(LocalDateTime.now());
                task.setUpdatedAt(LocalDateTime.now());
                pdfTaskMapper.insert(task);
                auditLogService.record(userId, "pdf_export", resumeId, "templateId=" + exportTemplateId);

                // 异步导出必须在事务提交后触发：事务内立即提交线程池时，后台线程可能读不到未提交的任务行
                submitExportAfterCommit(task.getId(), userId, resumeId, exportTemplateId);

                Map<String, Object> result = new java.util.HashMap<>();
                result.put("taskId", task.getId());
                result.put("status", task.getStatus());
                return result;
            }
        } finally {
            // 不 remove：同 AiResumeOptimizeService ABA 原因，去重锁常驻；key 为 user:resume 有界。
        }
    }

    /**
     * 查找同 (userId, resumeId) 仍处于 pending/processing 的导出任务（用于重复提交去重）。
     * 可能存在多条历史遗留的进行中任务，取最新一条即可。
     */
    private PdfTask findActiveTask(String userId, String resumeId) {
        LambdaQueryWrapper<PdfTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PdfTask::getUserId, userId)
                .eq(PdfTask::getResumeId, resumeId)
                .in(PdfTask::getStatus,
                        List.of(BizConstant.TASK_STATUS_PENDING, BizConstant.TASK_STATUS_PROCESSING))
                .orderByDesc(PdfTask::getCreatedAt);
        List<PdfTask> tasks = pdfTaskMapper.selectList(wrapper);
        return tasks.isEmpty() ? null : tasks.get(0);
    }

    /**
     * 管理端创建 PDF 导出任务：不做所有权校验，任务归属简历所有者，
     * 所有者可在自己的下载中心查看，完成时收到通知。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> exportPdfByAdmin(String operatorId, String resumeId, String templateId) {
        log.info("exportPdfByAdmin start: operatorId={}, resumeId={}, templateId={}",
                operatorId, resumeId, templateId);
        Resume resume = resumeService.getResumeForPreview(resumeId);
        validateResumeForExport(resume);

        String exportTemplateId = StringUtils.isNotBlank(templateId) ? templateId : resume.getTemplateId();
        // 渲染场景容忍 inactive/deleted 模板
        templateService.getTemplateEntityForRender(exportTemplateId);

        PdfTask task = new PdfTask();
        task.setUserId(resume.getUserId());
        task.setResumeId(resumeId);
        task.setTemplateId(exportTemplateId);
        task.setStatus(BizConstant.TASK_STATUS_PENDING);
        task.setDeleted(BizConstant.NOT_DELETED);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        pdfTaskMapper.insert(task);
        auditLogService.record(operatorId, "pdf_export_admin", resumeId, "templateId=" + exportTemplateId);

        submitExportAfterCommit(task.getId(), task.getUserId(), resumeId, exportTemplateId);

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("taskId", task.getId());
        result.put("status", task.getStatus());
        return result;
    }

    /**
     * 事务提交后再提交后台导出；无事务上下文时立即提交。
     * 队列满时将任务标记为失败，避免永久卡在 pending。
     */
    private void submitExportAfterCommit(String taskId, String userId, String resumeId, String exportTemplateId) {
        Runnable submit = () -> {
            // 信号量在线程池外获取，避免占满 4 个 pdfTaskExecutor 线程同时等待导致的死锁
            boolean acquired;
            try {
                acquired = exportSemaphore.tryAcquire(SEMAPHORE_WAIT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                markTaskFailed(taskId, "导出被中断。");
                return;
            }
            if (!acquired) {
                log.warn("PDF export concurrency limit reached before queueing: taskId={}", taskId);
                markTaskFailed(taskId, "导出任务繁忙，请稍后再试。");
                return;
            }
            try {
                pdfTaskExecutor.execute(() -> {
                    try {
                        executeExport(taskId, userId, resumeId, exportTemplateId);
                    } finally {
                        exportSemaphore.release();
                    }
                });
            } catch (RejectedExecutionException e) {
                exportSemaphore.release();
                log.warn("PDF export queue full: taskId={}", taskId);
                markTaskFailed(taskId, "导出任务过多，请稍后再试。");
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    submit.run();
                }
            });
        } else {
            submit.run();
        }
    }

    /**
     * 后台执行 PDF 导出（信号量已在外层获取，此处仅执行业务）。
     */
    public void executeExport(String taskId, String userId, String resumeId, String exportTemplateId) {
        try {
            Resume resume = resumeService.getResumeEntity(userId, resumeId);
            // 渲染场景容忍 inactive/deleted 模板，历史简历仍可导出
            Template template = templateService.getTemplateEntityForRender(exportTemplateId);
            generatePdf(taskId, resume, template);
            resumeService.incrementExportCount(resumeId);
        } catch (BusinessException e) {
            log.warn("PDF export failed: taskId={}, cause={}", taskId, e.getMessage());
            markTaskFailed(taskId, e.getMessage());
        } catch (Exception e) {
            log.error("PDF export failed: taskId={}", taskId, e);
            markTaskFailed(taskId, "PDF 生成失败，请稍后再试。");
        }
    }

    private void markTaskFailed(String taskId, String errorMsg) {
        // CAS：仅 pending/processing 可转 failed，已 success 的不再覆盖
        LambdaUpdateWrapper<PdfTask> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(PdfTask::getId, taskId)
                .in(PdfTask::getStatus, List.of(BizConstant.TASK_STATUS_PENDING, BizConstant.TASK_STATUS_PROCESSING))
                .set(PdfTask::getStatus, BizConstant.TASK_STATUS_FAILED)
                .set(PdfTask::getErrorMsg, errorMsg)
                .set(PdfTask::getUpdatedAt, LocalDateTime.now());
        pdfTaskMapper.update(null, wrapper);
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
     * 管理端查询 PDF 任务（不做所有权校验）。
     */
    public PdfTaskResponse getTaskByAdmin(String taskId) {
        PdfTask task = pdfTaskMapper.selectById(taskId);
        if (task == null || BizConstant.DELETED.equals(task.getDeleted())) {
            throw new BusinessException(ResultCode.PDF_TASK_NOT_FOUND, "PDF 任务不存在。");
        }
        return toResponse(task);
    }

    /**
     * 管理端流式下载 PDF（不做所有权校验）。
     */
    public InputStream downloadPdfStreamByAdmin(String taskId) {
        PdfTask task = pdfTaskMapper.selectById(taskId);
        if (task == null || BizConstant.DELETED.equals(task.getDeleted())) {
            throw new BusinessException(ResultCode.PDF_TASK_NOT_FOUND, "PDF 任务不存在。");
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
     * <p>
     * 任务记录在当前事务内逻辑删除；MinIO 文件延迟到事务提交后移除，
     * 若事务回滚则文件保留，避免出现"记录在、文件没了"的不一致。
     * </p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void cleanupTasksByResume(String userId, String resumeId) {
        LambdaQueryWrapper<PdfTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PdfTask::getUserId, userId)
                .eq(PdfTask::getResumeId, resumeId);
        List<PdfTask> tasks = pdfTaskMapper.selectList(wrapper);
        List<String> filePaths = tasks.stream()
                .map(PdfTask::getFilePath)
                .filter(StringUtils::isNotBlank)
                .toList();
        if (!tasks.isEmpty()) {
            pdfTaskMapper.delete(wrapper);
        }
        minioStorageService.removeAfterCommit(minioStorageService.getBucketPdfs(), filePaths);
    }

    private void generatePdf(String taskId, Resume resume, Template template) {
        Path tempDir = null;
        Browser browser = null;
        try {
            updateTaskStatus(taskId, BizConstant.TASK_STATUS_PROCESSING, null);
            log.info("generatePdf -> processing: taskId={}, resumeId={}", taskId, resume.getId());

            String html = resumeRenderService.render(resume, template);
            String fileName = buildFileName(resume);
            tempDir = Files.createTempDirectory("resume-pdf-");
            Path htmlPath = tempDir.resolve("resume.html");
            Path pdfPath = tempDir.resolve(fileName);
            Files.writeString(htmlPath, html);

            browser = acquireBrowser();
            try (BrowserContext context = browser.newContext();
                 Page page = context.newPage()) {
                page.navigate(htmlPath.toUri().toString(),
                        new Page.NavigateOptions().setTimeout(60_000));
                // 等待网络空闲，确保头像等图片加载完成再截图
                try {
                    page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE,
                            new Page.WaitForLoadStateOptions().setTimeout(10_000));
                } catch (Exception e) {
                    log.warn("waitForLoadState timeout, continue to pdf: taskId={}", taskId);
                }
                applyOnePageFit(page);
                page.pdf(new Page.PdfOptions().setPath(pdfPath)
                        .setFormat("A4")
                        .setPrintBackground(true));
            }

            String objectName = resume.getUserId() + "/pdfs/" + taskId + "/" + fileName;
            try (InputStream in = new FileInputStream(pdfPath.toFile())) {
                minioStorageService.upload(minioStorageService.getBucketPdfs(), objectName,
                        in, pdfPath.toFile().length(), "application/pdf");
            }

            // CAS：仅 pending/processing 可转 success；若 sweeper 已置 failed 则不再复活
            LambdaUpdateWrapper<PdfTask> successWrapper = new LambdaUpdateWrapper<>();
            successWrapper.eq(PdfTask::getId, taskId)
                    .in(PdfTask::getStatus, List.of(BizConstant.TASK_STATUS_PENDING, BizConstant.TASK_STATUS_PROCESSING))
                    .set(PdfTask::getFilePath, objectName)
                    .set(PdfTask::getFileName, fileName)
                    .set(PdfTask::getFileSize, pdfPath.toFile().length())
                    .set(PdfTask::getStatus, BizConstant.TASK_STATUS_SUCCESS)
                    .set(PdfTask::getCompletedAt, LocalDateTime.now())
                    .set(PdfTask::getUpdatedAt, LocalDateTime.now());
            Integer successAffected = pdfTaskMapper.update(null, successWrapper);
            if (successAffected == null || successAffected == 0) {
                log.warn("PDF task success CAS missed (swept to failed?): taskId={}", taskId);
                return;
            }
            try {
                notificationService.notify(resume.getUserId(), "pdf", "PDF 导出完成",
                        "你的简历《" + resume.getTitle() + "》已导出为 PDF，可前往下载中心下载。");
            } catch (Exception ne) {
                log.warn("PDF success notification failed, ignore: taskId={}", taskId, ne);
            }
            log.info("generatePdf success: taskId={}, fileSize={}, fileName={}",
                    taskId, pdfPath.toFile().length(), fileName);
        } catch (Exception e) {
            log.error("Generate PDF failed: taskId={}", taskId, e);
            // Chromium 崩溃/关闭后丢弃共享实例，下次导出时重建，避免复用已损坏的浏览器
            // 注意：browser 为方法内局部变量，需与共享实例比对，避免误丢新实例
            if (browser != null) {
                synchronized (browserLock) {
                    if (browser == sharedBrowser && !browser.isConnected()) {
                        log.warn("Shared Chromium seems crashed, discarding for next export");
                        discardLocked();
                    } else if (browser != sharedBrowser && !browser.isConnected()) {
                        try { browser.close(); } catch (Exception ignore) {}
                    }
                }
            }
            String msg = e.getMessage() == null ? "" : e.getMessage();
            updateTaskStatus(taskId, BizConstant.TASK_STATUS_FAILED,
                    StringUtils.abbreviate("PDF 生成失败：" + msg, 500));
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
        }
    }

    /**
     * 获取共享 Chromium 浏览器；未启动或已崩溃时（重新）启动。
     * Playwright 的 BrowserContext 相互隔离，支持并发导出共享同一浏览器进程。
     */
    private Browser acquireBrowser() {
        Browser current = sharedBrowser;
        if (current != null && current.isConnected()) {
            return current;
        }
        synchronized (browserLock) {
            current = sharedBrowser;
            if (current != null && !current.isConnected()) {
                discardLocked();
            }
            if (sharedBrowser == null) {
                Playwright pw = null;
                try {
                    pw = Playwright.create();
                    Browser br = pw.chromium().launch(buildLaunchOptions());
                    sharedPlaywright = pw;
                    sharedBrowser = br;
                    log.info("Shared Chromium launched for PDF export");
                } catch (Exception e) {
                    if (pw != null) {
                        try { pw.close(); } catch (Exception ignore) {}
                    }
                    throw e;
                }
            }
            return sharedBrowser;
        }
    }

    /**
     * 丢弃共享浏览器实例（加锁包装，供导出失败路径调用）。
     */
    private void discardBrowser() {
        synchronized (browserLock) {
            discardLocked();
        }
    }

    /**
     * 丢弃共享浏览器实例（须在 browserLock 内调用）。
     */
    private void discardLocked() {
        if (sharedBrowser != null) {
            try {
                sharedBrowser.close();
            } catch (Exception e) {
                log.warn("Failed to close crashed Chromium", e);
            }
            sharedBrowser = null;
        }
        if (sharedPlaywright != null) {
            try {
                sharedPlaywright.close();
            } catch (Exception e) {
                log.warn("Failed to close Playwright after browser crash", e);
            }
            sharedPlaywright = null;
        }
    }

    /**
     * 应用关闭时释放共享浏览器资源。
     */
    @jakarta.annotation.PreDestroy
    public void shutdownBrowser() {
        synchronized (browserLock) {
            discardLocked();
        }
    }

    /**
     * 将开启一页适配的简历内容按 A4 高度缩放，避免 PDF 产生第二页。
     * 页面默认不缩放，只有渲染 HTML 标记了 data-auto-one-page=true 时才执行。
     */
    private void applyOnePageFit(Page page) {
        page.evaluate("() => {"
                + "const resumePage = document.querySelector('.resume-page[data-auto-one-page=\\\"true\\\"]');"
                + "if (!resumePage) return;"
                + "resumePage.style.setProperty('--resume-fit-scale', '1');"
                + "const originalMinHeight = resumePage.style.minHeight;"
                + "resumePage.style.minHeight = '0';"
                + "const contentHeight = resumePage.scrollHeight;"
                + "resumePage.style.minHeight = originalMinHeight;"
                + "const a4Height = 297 * 96 / 25.4;"
                + "const scale = Math.min(1, a4Height / Math.max(contentHeight, 1));"
                + "resumePage.style.setProperty('--resume-fit-scale', String(scale));"
                + "}");
    }

    private void updateTaskStatus(String taskId, String status, String errorMsg) {
        // CAS：processing 推进仅当仍为 pending；失败终态仅当仍为 pending/processing
        LambdaUpdateWrapper<PdfTask> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(PdfTask::getId, taskId);
        if (BizConstant.TASK_STATUS_PROCESSING.equals(status)) {
            wrapper.eq(PdfTask::getStatus, BizConstant.TASK_STATUS_PENDING);
        } else {
            wrapper.in(PdfTask::getStatus, List.of(BizConstant.TASK_STATUS_PENDING, BizConstant.TASK_STATUS_PROCESSING));
        }
        wrapper.set(PdfTask::getStatus, status)
                .set(PdfTask::getErrorMsg, errorMsg)
                .set(PdfTask::getUpdatedAt, LocalDateTime.now());
        Integer affected = pdfTaskMapper.update(null, wrapper);
        if ((affected == null || affected == 0) && BizConstant.TASK_STATUS_PROCESSING.equals(status)) {
            log.warn("PDF task status CAS missed (already swept?): taskId={}, target={}", taskId, status);
        }
    }

    private void validateResumeForExport(Resume resume) {
        resumeSectionValidator.validateForExport(resume.getSections());
    }

    private String buildFileName(Resume resume) {
        List<SectionDTO> sections = resume.getSections() != null ? resume.getSections() : List.of();
        String name = "";
        String targetPosition = sanitizeFileName(resume.getTargetPosition());

        SectionDTO profileSection = sections.stream()
                .filter(s -> BizConstant.SECTION_TYPE_PROFILE.equals(s.getType()))
                .findFirst()
                .orElse(null);
        if (profileSection != null && profileSection.getData() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> profile = (Map<String, Object>) profileSection.getData();
            name = sanitizeFileName(getString(profile, "name"));
        }

        if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(targetPosition)) {
            return truncateFileName(name + "_" + targetPosition + "_简历.pdf");
        }
        if (StringUtils.isNotBlank(name)) {
            return truncateFileName(name + "_简历.pdf");
        }
        return "我的简历_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";
    }

    /**
     * 清理文件名中的路径分隔符与控制字符，避免拼进 MinIO 对象键时产生歧义。
     */
    private String sanitizeFileName(String name) {
        if (name == null) {
            return "";
        }
        return name.replaceAll("[\\\\/:*?\"<>|\\r\\n\\t]", "_").trim();
    }

    /**
     * 截断文件名，避免超出数据库 file_name VARCHAR(128) 列宽。
     */
    private String truncateFileName(String fileName) {
        if (fileName == null || fileName.length() <= 100) {
            return fileName;
        }
        return fileName.substring(0, 97) + ".pdf";
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
