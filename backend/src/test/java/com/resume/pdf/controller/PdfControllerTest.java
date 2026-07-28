package com.resume.pdf.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.common.config.TestSecurityConfig;
import com.resume.pdf.dto.ExportPdfRequest;
import com.resume.pdf.dto.PdfTaskResponse;
import com.resume.pdf.service.PdfService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PdfController.class)
@Import(TestSecurityConfig.class)
class PdfControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PdfService pdfService;

    @Test
    void export_shouldReturnTaskId() throws Exception {
        ExportPdfRequest request = new ExportPdfRequest();
        request.setResumeId("resume_1");

        when(pdfService.exportPdf(any(), eq("resume_1"), any()))
                .thenReturn(Map.of("taskId", "pdf_1", "status", "success"));

        mockMvc.perform(post("/pdf/export")
                .with(csrf())
                .with(user("user123").roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").value("pdf_1"));
    }

    @Test
    void export_shouldRejectMissingResumeId() throws Exception {
        ExportPdfRequest request = new ExportPdfRequest();
        // resumeId is null

        mockMvc.perform(post("/pdf/export")
                .with(csrf())
                .with(user("user123").roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTask_shouldReturnTaskDetail() throws Exception {
        PdfTaskResponse response = new PdfTaskResponse();
        response.setTaskId("pdf_1");
        response.setResumeId("resume_1");
        response.setStatus("success");
        response.setFileName("resume.pdf");

        when(pdfService.getTask(any(), eq("pdf_1"))).thenReturn(response);

        mockMvc.perform(get("/pdf/tasks/pdf_1")
                .with(csrf())
                .with(user("user123").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").value("pdf_1"));
    }

    @Test
    void download_shouldStreamBytes() throws Exception {
        PdfTaskResponse task = new PdfTaskResponse();
        task.setTaskId("pdf_1");
        task.setFileName("resume.pdf");
        task.setStatus("success");

        byte[] pdfBytes = "%PDF-1.4 fake pdf content".getBytes();
        when(pdfService.getTask(any(), eq("pdf_1"))).thenReturn(task);
        when(pdfService.downloadPdfStream(any(), eq("pdf_1")))
                .thenReturn(new ByteArrayInputStream(pdfBytes));

        mockMvc.perform(get("/pdf/download/pdf_1")
                .with(csrf())
                .with(user("user123").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().exists("Content-Disposition"));
    }
}