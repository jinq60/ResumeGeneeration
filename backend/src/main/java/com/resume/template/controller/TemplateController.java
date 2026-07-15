package com.resume.template.controller;

import com.resume.common.entity.R;
import com.resume.template.dto.TemplateDTO;
import com.resume.template.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 前台模板相关接口。
 */
@RestController
@RequestMapping("/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @GetMapping
    public R<List<TemplateDTO>> list() {
        return R.success(templateService.listActiveTemplates());
    }

    @GetMapping("/{id}")
    public R<TemplateDTO> get(@PathVariable String id) {
        return R.success(templateService.getTemplate(id));
    }
}
