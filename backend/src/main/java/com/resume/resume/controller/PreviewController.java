package com.resume.resume.controller;

import com.resume.common.service.ResumeRenderService;
import com.resume.resume.entity.Resume;
import com.resume.resume.service.ResumeService;
import com.resume.template.entity.Template;
import com.resume.template.service.TemplateService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 简历实时预览接口。
 *
 * <p>
 * 通过服务端统一渲染 HTML，前端以 iframe 加载，确保预览与 PDF 导出视觉一致。
 * </p>
 */
@RestController
@RequestMapping("/resumes")
@RequiredArgsConstructor
public class PreviewController {

    private final ResumeService resumeService;
    private final TemplateService templateService;
    private final ResumeRenderService resumeRenderService;

    /**
     * 预览简历 HTML。
     *
     * @param userId     当前用户 ID（匿名访问时可为 null）
     * @param resumeId   简历 ID
     * @param templateId 可选，覆盖本次预览使用的模板（不修改简历）
     * @param response   HTTP 响应
     */
    @GetMapping("/{resumeId}/preview")
    public void preview(@PathVariable String resumeId,
                        @RequestParam(required = false) String templateId,
                        HttpServletResponse response) throws IOException {
        Resume resume = resumeService.getResumeForPreview(resumeId);

        String previewTemplateId = StringUtils.isNotBlank(templateId) ? templateId : resume.getTemplateId();
        Template template = templateService.getTemplateEntity(previewTemplateId);

        String html = resumeRenderService.render(resume, template);
        response.setContentType(MediaType.TEXT_HTML_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(html);
    }
}
