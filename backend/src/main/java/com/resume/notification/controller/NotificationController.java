package com.resume.notification.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.resume.common.entity.R;
import com.resume.notification.dto.NotificationResponse;
import com.resume.notification.service.NotificationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 通知中心接口。
 */
@Validated
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public R<Page<NotificationResponse>> list(@AuthenticationPrincipal String userId,
                                              @RequestParam(defaultValue = "1") @Min(1) int page,
                                              @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
                                              @RequestParam(defaultValue = "false") boolean unreadOnly) {
        return R.success(notificationService.list(userId, page, size, unreadOnly));
    }

    @GetMapping("/unread-count")
    public R<Map<String, Long>> unreadCount(@AuthenticationPrincipal String userId) {
        return R.success(Map.of("count", notificationService.unreadCount(userId)));
    }

    @PutMapping("/{id}/read")
    public R<Void> markRead(@AuthenticationPrincipal String userId,
                            @PathVariable String id) {
        notificationService.markRead(userId, id);
        return R.success();
    }

    @PutMapping("/read-all")
    public R<Void> markAllRead(@AuthenticationPrincipal String userId) {
        notificationService.markAllRead(userId);
        return R.success();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@AuthenticationPrincipal String userId,
                          @PathVariable String id) {
        notificationService.delete(userId, id);
        return R.success();
    }
}