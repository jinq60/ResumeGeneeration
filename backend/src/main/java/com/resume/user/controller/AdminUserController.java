package com.resume.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.resume.common.entity.R;
import com.resume.user.dto.AdminCreateUserRequest;
import com.resume.user.dto.AdminCreateUserResponse;
import com.resume.user.dto.AdminUserDetailResponse;
import com.resume.user.dto.AdminUserListItemResponse;
import com.resume.user.dto.ResetPasswordResponse;
import com.resume.user.dto.UpdateUserStatusRequest;
import com.resume.user.dto.UserStatsResponse;
import com.resume.user.service.AdminUserService;
import com.resume.user.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
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
    private final UserService userService;

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
    public R<Void> updateStatus(@AuthenticationPrincipal String operatorId,
                                @PathVariable String id,
                                @Valid @RequestBody UpdateUserStatusRequest request) {
        adminUserService.updateStatus(id, request.getStatus(), operatorId);
        return R.success();
    }

    /**
     * 设置用户角色（USER / ADMIN）。
     */
    @PatchMapping("/{id}/role")
    public R<Void> updateRole(@AuthenticationPrincipal String operatorId,
                              @PathVariable String id,
                              @RequestBody Map<String, String> body) {
        adminUserService.updateRole(id, body.get("role"), operatorId);
        return R.success();
    }

    /**
     * 重置用户密码，返回一次性临时密码。
     */
    @PostMapping("/{id}/reset-password")
    public R<ResetPasswordResponse> resetPassword(@AuthenticationPrincipal String operatorId,
                                                  @PathVariable String id) {
        return R.success(adminUserService.resetPassword(id, operatorId));
    }

    /**
     * 逻辑删除用户。
     */
    @DeleteMapping("/{id}")
    public R<Void> delete(@AuthenticationPrincipal String operatorId, @PathVariable String id) {
        adminUserService.deleteUser(id, operatorId);
        return R.success();
    }

    /**
     * 新增用户（邮箱必填；初始密码为空时生成一次性临时密码）。
     */
    @PostMapping
    public R<AdminCreateUserResponse> create(@Valid @RequestBody AdminCreateUserRequest request) {
        return R.success(userService.adminCreateUser(request));
    }

    /**
     * 导出用户 CSV。
     */
    @GetMapping("/export")
    public void export(@RequestParam(required = false) String status,
                       @RequestParam(required = false) String keyword,
                       HttpServletResponse response) throws IOException {
        String csv = adminUserService.buildCsv(status, keyword);
        String fileName = URLEncoder.encode("用户数据_" + LocalDate.now() + ".csv", StandardCharsets.UTF_8)
                .replace("+", "%20");
        response.setContentType("text/csv;charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);
        response.getOutputStream().write(0xEF);
        response.getOutputStream().write(0xBB);
        response.getOutputStream().write(0xBF);
        response.getOutputStream().write(csv.getBytes(StandardCharsets.UTF_8));
    }
}