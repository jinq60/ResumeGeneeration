package com.resume.user.controller;

import com.resume.common.entity.R;
import com.resume.user.dto.ChangePasswordRequest;
import com.resume.user.dto.UserInfoResponse;
import com.resume.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
     * 修改当前用户密码（成功后旧会话全部失效，需重新登录）。
     */
    @PutMapping("/me/password")
    public R<Void> changePassword(@AuthenticationPrincipal String userId,
                                  @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userId, request);
        return R.success();
    }
}
