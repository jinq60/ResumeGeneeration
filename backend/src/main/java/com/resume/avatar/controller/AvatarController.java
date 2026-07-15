package com.resume.avatar.controller;

import com.resume.common.entity.R;
import com.resume.avatar.dto.AvatarTaskResponse;
import com.resume.avatar.dto.AvatarUploadResponse;
import com.resume.avatar.dto.OptimizeAvatarRequest;
import com.resume.avatar.service.AvatarService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 头像相关接口。
 */
@RestController
@RequestMapping("/avatars")
@RequiredArgsConstructor
public class AvatarController {

    private final AvatarService avatarService;

    @PostMapping("/upload")
    public R<AvatarUploadResponse> upload(@AuthenticationPrincipal String userId,
                                          @RequestParam("file") MultipartFile file,
                                          @RequestParam(required = false) String resumeId) {
        return R.success(avatarService.uploadAvatar(userId, file, resumeId));
    }

    @PostMapping("/optimize")
    public R<Map<String, Object>> optimize(@AuthenticationPrincipal String userId,
                                            @Valid @RequestBody OptimizeAvatarRequest request) {
        return R.success(avatarService.optimizeAvatar(userId, request));
    }

    @GetMapping("/tasks/{taskId}")
    public R<AvatarTaskResponse> getTask(@AuthenticationPrincipal String userId,
                                          @PathVariable String taskId) {
        return R.success(avatarService.getTask(userId, taskId));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@AuthenticationPrincipal String userId,
                          @PathVariable String id) {
        avatarService.deleteAvatar(userId, id);
        return R.success();
    }
}
