package com.resume.template.controller;

import com.resume.common.entity.R;
import com.resume.template.dto.TemplateDTO;
import com.resume.template.service.TemplateService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 前台模板相关接口。
 */
@RestController
@RequestMapping("/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @GetMapping
    public ResponseEntity<R<List<TemplateDTO>>> list(HttpServletRequest request) {
        List<TemplateDTO> list = templateService.listActiveTemplates();
        String etag = "\"" + Integer.toHexString(list.hashCode()) + "-" + list.size() + "\"";
        String ifNoneMatch = request.getHeader("If-None-Match");
        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(304).eTag(etag).build();
        }
        return ResponseEntity.ok()
                .eTag(etag)
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic())
                .body(R.success(list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<R<TemplateDTO>> get(@PathVariable String id, HttpServletRequest request) {
        TemplateDTO dto = templateService.getTemplate(id);
        String etag = "\"" + id + "-" + (dto.getUpdatedAt() != null ? dto.getUpdatedAt().hashCode() : 0) + "\"";
        String ifNoneMatch = request.getHeader("If-None-Match");
        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(304).eTag(etag).build();
        }
        return ResponseEntity.ok()
                .eTag(etag)
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic())
                .body(R.success(dto));
    }
}
