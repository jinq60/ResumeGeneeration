package com.resume.delivery.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.resume.common.entity.R;
import com.resume.delivery.dto.DeliveryRecordResponse;
import com.resume.delivery.dto.DeliveryStatsResponse;
import com.resume.delivery.service.DeliveryService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * 管理端投递数据接口（需 ADMIN 角色）。
 */
@Validated
@RestController
@RequestMapping("/admin/deliveries")
@RequiredArgsConstructor
public class AdminDeliveryController {

    private final DeliveryService deliveryService;

    @GetMapping
    public R<Page<DeliveryRecordResponse>> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String keyword) {
        return R.success(deliveryService.listAdmin(page, size, keyword));
    }

    @GetMapping("/stats")
    public R<DeliveryStatsResponse> stats() {
        return R.success(deliveryService.stats());
    }

    /**
     * 导出投递数据 CSV。
     */
    @GetMapping("/export")
    public void export(HttpServletResponse response) throws IOException {
        String csv = deliveryService.buildCsv();
        String fileName = URLEncoder.encode("投递数据_" + LocalDate.now() + ".csv", StandardCharsets.UTF_8)
                .replace("+", "%20");
        response.setContentType("text/csv;charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);
        // 加 BOM 便于 Excel 正确识别 UTF-8 中文
        response.getOutputStream().write(0xEF);
        response.getOutputStream().write(0xBB);
        response.getOutputStream().write(0xBF);
        response.getOutputStream().write(csv.getBytes(StandardCharsets.UTF_8));
    }
}