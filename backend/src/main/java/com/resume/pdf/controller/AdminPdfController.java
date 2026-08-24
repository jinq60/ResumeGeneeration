package com.resume.pdf.controller;

import com.resume.common.entity.R;
import com.resume.pdf.dto.PdfTaskResponse;
import com.resume.pdf.service.PdfService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 管理端 PDF 任务接口（需 ADMIN 角色）。
 */
@Slf4j
@RestController
@RequestMapping("/admin/pdf")
@RequiredArgsConstructor
public class AdminPdfController {

    private final PdfService pdfService;

    @GetMapping("/tasks/{taskId}")
    public R<PdfTaskResponse> getTask(@AuthenticationPrincipal String operatorId,
                                      @PathVariable String taskId) {
        return R.success(pdfService.getTaskByAdmin(taskId));
    }

    @GetMapping("/download/{taskId}")
    public StreamingResponseBody download(@AuthenticationPrincipal String operatorId,
                                          @PathVariable String taskId,
                                          HttpServletResponse response) {
        PdfTaskResponse task = pdfService.getTaskByAdmin(taskId);
        InputStream inputStream = pdfService.downloadPdfStreamByAdmin(taskId);

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
                log.warn("Admin PDF streaming download interrupted: taskId={}", taskId, e);
            }
        };
    }
}