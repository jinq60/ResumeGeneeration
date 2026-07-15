package com.resume.user.controller;

import com.resume.common.entity.R;
import com.resume.user.dto.UserInfoResponse;
import com.resume.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
