package com.resume.pdf.controller;

import com.resume.common.entity.R;
import com.resume.pdf.dto.ExportPdfRequest;
import com.resume.pdf.dto.PdfTaskResponse;
import com.resume.pdf.service.PdfService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * PDF 导出相关接口。
 */
@Slf4j
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

    /**
     * 流式下载 PDF，避免一次性读入内存导致大文件 OOM。
     */
    @GetMapping("/download/{taskId}")
    public StreamingResponseBody download(@AuthenticationPrincipal String userId,
                                            @PathVariable String taskId,
                                            HttpServletResponse response) {
        PdfTaskResponse task = pdfService.getTask(userId, taskId);
        InputStream inputStream = pdfService.downloadPdfStream(userId, taskId);

        response.setContentType(MediaType.APPLICATION_PDF_VALUE);
        String encodedFileName = URLEncoder.encode(task.getFileName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + encodedFileName);

        return outputStream -> {
            try (inputStream) {
                byte[] buffer = new byte[8 * 1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            } catch (Exception e) {
                log.warn("PDF streaming download interrupted: taskId={}", taskId, e);
            }
        };
    }
}
