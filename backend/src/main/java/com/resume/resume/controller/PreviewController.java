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
import org.springframework.beans.factory.annotation.Value;
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
     * 允许嵌入预览 iframe 的前端来源（取配置的第一个），避免硬编码开发环境地址。
     */
    @Value("${app.cors.allowed-origins:}")
    private String allowedOrigins;

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
        // 渲染场景容忍 inactive/deleted 模板，历史简历仍可预览
        Template template = templateService.getTemplateEntityForRender(previewTemplateId);

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
        // 限制请求体大小，防止超大 JSON 消耗内存/CPU；ContentLengthLong==-1 (chunked) 时需额外校验 sections 序列化长度
        long contentLength = servletRequest.getContentLengthLong();
        if (contentLength > MAX_REQUEST_BODY_BYTES) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "预览数据过大。");
        }
        if (contentLength == -1) {
            // chunked 场景：通过 validator 的 MAX_TOTAL_CONTENT_LENGTH 二次兜底，但此处先做粗略 JSON 长度估计
            try {
                String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request.getResume().getSections());
                if (json.length() > MAX_REQUEST_BODY_BYTES) {
                    throw new BusinessException(ResultCode.PARAM_INVALID, "预览数据过大。");
                }
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "简历数据格式错误。");
            }
        }
        Resume resume = request.getResume();
        // 结构与长度校验（与保存/导出口径一致）
        resumeSectionValidator.validateDraft(resume.getSections());

        String previewTemplateId = StringUtils.isNotBlank(request.getTemplateId())
                ? request.getTemplateId() : resume.getTemplateId();
        // 渲染场景容忍 inactive/deleted 模板（如简历引用的模板刚被下架/删除）
        Template template = templateService.getTemplateEntityForRender(previewTemplateId);

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
                "default-src 'none'; " + buildImgSrcCsp()
                        + "; style-src 'unsafe-inline'; frame-ancestors " + buildFrameAncestors());
        response.getWriter().write(html);
    }

    private String buildImgSrcCsp() {
        String origin = resumeRenderService.publicBaseOrigin();
        return "img-src 'self' data:" + (origin != null ? " " + origin : "");
    }

    /**
     * frame-ancestors 指令值：'self' 加上配置的前端来源（app.cors.allowed-origins 第一个）。
     */
    private String buildFrameAncestors() {
        return "'self'" + firstAllowedOrigin().map(" "::concat).orElse("");
    }

    private java.util.Optional<String> firstAllowedOrigin() {
        return java.util.Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .findFirst();
    }
}
