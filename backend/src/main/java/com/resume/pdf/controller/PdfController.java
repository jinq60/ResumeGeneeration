package com.resume.pdf.controller;

import com.resume.common.constant.ResultCode;
import com.resume.common.entity.R;
import com.resume.common.exception.BusinessException;
import com.resume.pdf.dto.ExportPdfRequest;
import com.resume.pdf.dto.PdfTaskResponse;
import com.resume.pdf.service.PdfService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * PDF 导出相关接口。
 */
@RestController
@RequestMapping("/pdf")
@RequiredArgsConstructor
public class PdfController {

    private final PdfService pdfService;

    @PostMapping("/export")
    public R<Map<String, Object>> export(@AuthenticationPrincipal String userId,
                                        @Valid @RequestBody ExportPdfRequest request) {
        return R.success(pdfService.exportPdf(userId, request.getResumeId(), request.getTemplateId()));
    }

    @GetMapping("/tasks/{taskId}")
    public R<PdfTaskResponse> getTask(@AuthenticationPrincipal String userId,
                                       @PathVariable String taskId) {
        return R.success(pdfService.getTask(userId, taskId));
    }

    @GetMapping("/download/{taskId}")
    public void download(@AuthenticationPrincipal String userId,
                         @PathVariable String taskId,
                         HttpServletResponse response) {
        PdfTaskResponse task = pdfService.getTask(userId, taskId);
        byte[] content = pdfService.downloadPdf(userId, taskId);

        response.setContentType(MediaType.APPLICATION_PDF_VALUE);
        String encodedFileName = URLEncoder.encode(task.getFileName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + encodedFileName);
        try (OutputStream out = response.getOutputStream()) {
            out.write(content);
            out.flush();
        } catch (Exception e) {
            throw new BusinessException(ResultCode.PDF_EXPORT_FAILED, "PDF 下载失败。");
        }
    }
}
