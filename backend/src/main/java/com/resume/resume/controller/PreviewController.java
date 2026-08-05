package com.resume.resume.controller;

import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.common.service.ResumeRenderService;
import com.resume.resume.dto.PreviewResumeRequest;
import com.resume.resume.entity.Resume;
import com.resume.resume.service.ResumeSectionValidator;
import com.resume.resume.service.ResumeService;
import com.resume.template.entity.Template;
import com.resume.template.service.TemplateService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
 * 预览接口需要认证，并校验当前用户是否为简历所有者，防止通过简历 ID 越权访问他人简历。
 * </p>
 */
@RestController
@RequestMapping("/resumes")
@RequiredArgsConstructor
public class PreviewController {

    private static final int MAX_REQUEST_BODY_BYTES = 512 * 1024;

    private final ResumeService resumeService;
    private final TemplateService templateService;
    private final ResumeRenderService resumeRenderService;
    private final ResumeSectionValidator resumeSectionValidator;

    /**
     * 预览简历 HTML。
     *
     * @param userId     当前用户 ID
     * @param resumeId   简历 ID
     * @param templateId 可选，覆盖本次预览使用的模板（不修改简历）
     * @param response   HTTP 响应
     */
    @GetMapping("/{resumeId}/preview")
    public void preview(@PathVariable String resumeId,
                        @RequestParam(required = false) String templateId,
                        @AuthenticationPrincipal String userId,
                        HttpServletResponse response) throws IOException {
        Resume resume = resumeService.getResumeEntity(userId, resumeId);

        String previewTemplateId = StringUtils.isNotBlank(templateId) ? templateId : resume.getTemplateId();
        Template template = templateService.getTemplateEntity(previewTemplateId);

        String html = resumeRenderService.render(resume, template);
        writeHtml(response, html);
    }

    /**
     * 实时预览：根据前端传入的简历对象直接渲染 HTML。
     * <p>
     * 用于编辑器未保存时即时查看效果，不读取数据库，不校验所有权。
     * 渲染前对 sections 做结构校验并限制请求体大小，防止超大 JSON 拖垮内存。
     * </p>
     */
    @PostMapping("/preview")
    public void previewLive(@RequestBody(required = false) PreviewResumeRequest request,
                            jakarta.servlet.http.HttpServletRequest servletRequest,
                            HttpServletResponse response) throws IOException {
        if (request == null || request.getResume() == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "简历数据不能为空。");
        }
        // 限制请求体大小，防止超大 JSON 消耗内存/CPU
        if (servletRequest.getContentLengthLong() > MAX_REQUEST_BODY_BYTES) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "预览数据过大。");
        }
        Resume resume = request.getResume();
        // 结构与长度校验（与保存/导出口径一致）
        resumeSectionValidator.validateDraft(resume.getSections());

        String previewTemplateId = StringUtils.isNotBlank(request.getTemplateId())
                ? request.getTemplateId() : resume.getTemplateId();
        Template template = templateService.getTemplateEntity(previewTemplateId);

        String html = resumeRenderService.render(resume, template);
        writeHtml(response, html);
    }

    private void writeHtml(HttpServletResponse response, String html) throws IOException {
        response.setContentType(MediaType.TEXT_HTML_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // 预览 HTML 含个人信息：禁止缓存
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        // iframe 内以 sandbox=allow-same-origin 加载，禁脚本执行；CSP 兜底
        response.setHeader("Content-Security-Policy",
                "default-src 'none'; img-src 'self' data:; style-src 'unsafe-inline'; frame-ancestors 'self' http://localhost:5173");
        response.getWriter().write(html);
    }
}
