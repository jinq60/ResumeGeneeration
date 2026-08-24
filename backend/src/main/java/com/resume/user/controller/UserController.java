package com.resume.user.controller;

import com.resume.common.entity.R;
import com.resume.user.dto.ChangePasswordRequest;
import com.resume.user.dto.UpdateProfileRequest;
import com.resume.user.dto.UserInfoResponse;
import com.resume.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户相关接口。
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public R<UserInfoResponse> me(@AuthenticationPrincipal String userId) {
        return R.success(userService.getCurrentUser(userId));
    }

    /**
     * 更新当前用户资料（昵称 / 手机号 / 邮箱 / 头像）。
     */
    @PutMapping("/me")
    public R<UserInfoResponse> updateProfile(@AuthenticationPrincipal String userId,
                                             @Valid @RequestBody UpdateProfileRequest request) {
        return R.success(userService.updateProfile(userId, request));
    }

    /**
     * 读取当前用户偏好设置。
     */
    @GetMapping("/me/preferences")
    public R<Map<String, Object>> getPreferences(@AuthenticationPrincipal String userId) {
        return R.success(userService.getPreferences(userId));
    }

    /**
     * 保存当前用户偏好设置。
     */
    @PutMapping("/me/preferences")
    public R<Map<String, Object>> savePreferences(@AuthenticationPrincipal String userId,
                                                  @RequestBody Map<String, Object> preferences) {
        return R.success(userService.savePreferences(userId, preferences));
    }

    /**
     * 修改当前用户密码（成功后旧会话全部失效，需重新登录）。
     */
    @PutMapping("/me/password")
    public R<Void> changePassword(@AuthenticationPrincipal String userId,
                                  @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userId, request);
        return R.success();
    }
}
