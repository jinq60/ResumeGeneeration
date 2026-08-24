package com.resume.resume.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.resume.common.entity.R;
import com.resume.common.service.ResumeRenderService;
import com.resume.pdf.service.PdfService;
import com.resume.resume.dto.AdminResumeListItemResponse;
import com.resume.resume.dto.AdminResumeStatsResponse;
import com.resume.resume.entity.Resume;
import com.resume.resume.service.ResumeExportService;
import com.resume.resume.service.ResumeService;
import com.resume.template.entity.Template;
import com.resume.template.service.TemplateService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 后台简历管理接口（需 ADMIN 角色）。
 */
@Validated
@RestController
@RequestMapping("/admin/resumes")
@RequiredArgsConstructor
public class AdminResumeController {

    private final ResumeService resumeService;
    private final ResumeExportService resumeExportService;
    private final ResumeRenderService resumeRenderService;
    private final TemplateService templateService;
    private final PdfService pdfService;

    @GetMapping
    public R<Page<AdminResumeListItemResponse>> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String keyword) {
        return R.success(resumeService.listAdminResumes(page, size, keyword));
    }

    @GetMapping("/stats")
    public R<AdminResumeStatsResponse> stats() {
        return R.success(resumeService.adminStats());
    }

    /**
     * 管理端预览简历 HTML（不做所有权校验，仅供审核 / 运营查看）。
     */
    @GetMapping("/{id}/preview")
    public void preview(@PathVariable String id, HttpServletResponse response) throws IOException {
        Resume resume = resumeService.getResumeForPreview(id);
        Template template = templateService.getTemplateEntity(resume.getTemplateId());
        String html = resumeRenderService.render(resume, template);
        response.setContentType(MediaType.TEXT_HTML_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Content-Security-Policy",
                "default-src 'none'; style-src 'unsafe-inline'; frame-ancestors 'self' http://localhost:5173");
        response.getWriter().write(html);
    }

    /**
     * 管理端导出 Markdown。
     */
    @GetMapping("/{id}/export/markdown")
    public void exportMarkdown(@PathVariable String id, HttpServletResponse response) throws IOException {
        String content = resumeExportService.buildMarkdownForAdmin(id);
        String fileName = resumeExportService.buildExportFileNameForAdmin(id, "md");
        writeDownload(response, content.getBytes(StandardCharsets.UTF_8),
                "text/markdown; charset=utf-8", fileName);
    }

    /**
     * 管理端导出 Word（docx）。
     */
    @GetMapping("/{id}/export/word")
    public void exportWord(@PathVariable String id, HttpServletResponse response) throws IOException {
        byte[] bytes = resumeExportService.buildWordForAdmin(id);
        String fileName = resumeExportService.buildExportFileNameForAdmin(id, "docx");
        writeDownload(response, bytes,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", fileName);
    }

    /**
     * 管理端创建 PDF 导出任务（任务归属简历所有者，所有者可在下载中心查看）。
     */
    @PostMapping("/{id}/export/pdf")
    public R<Map<String, Object>> exportPdf(@AuthenticationPrincipal String operatorId,
                                            @PathVariable String id,
                                            @RequestBody(required = false) Map<String, String> body) {
        String templateId = body == null ? null : body.get("templateId");
        return R.success(pdfService.exportPdfByAdmin(operatorId, id, templateId));
    }

    private void writeDownload(HttpServletResponse response, byte[] bytes, String contentType, String fileName)
            throws IOException {
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
    }
}
