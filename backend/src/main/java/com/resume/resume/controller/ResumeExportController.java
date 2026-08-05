package com.resume.resume.controller;

import com.resume.resume.service.ResumeExportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 简历多格式导出（Markdown / Word）。
 */
@RestController
@RequestMapping("/resumes")
@RequiredArgsConstructor
public class ResumeExportController {

    private final ResumeExportService resumeExportService;

    /**
     * 导出 Markdown。
     */
    @GetMapping("/{id}/export/markdown")
    public void exportMarkdown(@AuthenticationPrincipal String userId,
                               @PathVariable String id,
                               HttpServletResponse response) throws IOException {
        String content = resumeExportService.buildMarkdown(userId, id);
        String fileName = resumeExportService.buildExportFileName(userId, id, "md");
        writeDownload(response, content.getBytes(StandardCharsets.UTF_8),
                "text/markdown; charset=utf-8", fileName);
    }

    /**
     * 导出 Word（docx）。
     */
    @GetMapping("/{id}/export/word")
    public void exportWord(@AuthenticationPrincipal String userId,
                           @PathVariable String id,
                           HttpServletResponse response) throws IOException {
        byte[] bytes = resumeExportService.buildWord(userId, id);
        String fileName = resumeExportService.buildExportFileName(userId, id, "docx");
        writeDownload(response, bytes,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", fileName);
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
