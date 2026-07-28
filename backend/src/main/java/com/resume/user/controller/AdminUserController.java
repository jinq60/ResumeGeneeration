package com.resume.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.resume.common.entity.R;
import com.resume.user.dto.AdminUserDetailResponse;
import com.resume.user.dto.AdminUserListItemResponse;
import com.resume.user.dto.ResetPasswordResponse;
import com.resume.user.dto.UpdateUserStatusRequest;
import com.resume.user.dto.UserStatsResponse;
import com.resume.user.service.AdminUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 后台用户管理接口（需 ADMIN 角色）。
 */
@Validated
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * 后台用户列表。
     */
    @GetMapping
    public R<Page<AdminUserListItemResponse>> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return R.success(adminUserService.listUsers(page, size, status, keyword));
    }

    /**
     * 用户详情。
     */
    @GetMapping("/{id}")
    public R<AdminUserDetailResponse> get(@PathVariable String id) {
        return R.success(adminUserService.getUser(id));
    }

    /**
     * 用户统计。
     */
    @GetMapping("/stats")
    public R<UserStatsResponse> stats() {
        return R.success(adminUserService.stats());
    }

    /**
     * 更新用户状态（启用 / 禁用）。
     */
    @PatchMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable String id,
                                @Valid @RequestBody UpdateUserStatusRequest request) {
        adminUserService.updateStatus(id, request.getStatus());
        return R.success();
    }

    /**
     * 设置用户角色（USER / ADMIN）。
     */
    @PatchMapping("/{id}/role")
    public R<Void> updateRole(@PathVariable String id,
                              @RequestBody Map<String, String> body) {
        adminUserService.updateRole(id, body.get("role"));
        return R.success();
    }

    /**
     * 重置用户密码，返回一次性临时密码。
     */
    @PostMapping("/{id}/reset-password")
    public R<ResetPasswordResponse> resetPassword(@PathVariable String id) {
        return R.success(adminUserService.resetPassword(id));
    }

    /**
     * 逻辑删除用户。
     */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable String id) {
        adminUserService.deleteUser(id);
        return R.success();
    }
}