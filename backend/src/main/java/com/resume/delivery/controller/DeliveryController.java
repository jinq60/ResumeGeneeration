package com.resume.delivery.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.resume.common.entity.R;
import com.resume.delivery.dto.DeliveryRecordRequest;
import com.resume.delivery.dto.DeliveryRecordResponse;
import com.resume.delivery.service.DeliveryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 投递管理接口。
 */
@Validated
@RestController
@RequestMapping("/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    @PostMapping
    public R<DeliveryRecordResponse> create(@AuthenticationPrincipal String userId,
                                            @Valid @RequestBody DeliveryRecordRequest request) {
        return R.success(deliveryService.create(userId, request));
    }

    @GetMapping
    public R<Page<DeliveryRecordResponse>> list(@AuthenticationPrincipal String userId,
                                                @RequestParam(defaultValue = "1") @Min(1) int page,
                                                @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) String company,
                                                @RequestParam(required = false) String position,
                                                @RequestParam(required = false) String status,
                                                @RequestParam(required = false)
                                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                @RequestParam(required = false)
                                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.success(deliveryService.list(userId, page, size, keyword, company, position, status, startDate, endDate));
    }

    @GetMapping("/{id}")
    public R<DeliveryRecordResponse> get(@AuthenticationPrincipal String userId,
                                         @PathVariable String id) {
        return R.success(deliveryService.get(userId, id));
    }

    @PutMapping("/{id}")
    public R<DeliveryRecordResponse> update(@AuthenticationPrincipal String userId,
                                            @PathVariable String id,
                                            @Valid @RequestBody DeliveryRecordRequest request) {
        return R.success(deliveryService.update(userId, id, request));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@AuthenticationPrincipal String userId,
                          @PathVariable String id) {
        deliveryService.delete(userId, id);
        return R.success();
    }
}