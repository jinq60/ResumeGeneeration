package com.resume.avatar.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.avatar.dto.OptimizeAvatarRequest;
import com.resume.avatar.entity.AvatarTask;
import com.resume.avatar.mapper.AvatarTaskMapper;
import com.resume.common.constant.BizConstant;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.common.service.MinioStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvatarServiceTest {

    @Mock
    private AvatarTaskMapper avatarTaskMapper;

    @Mock
    private MinioStorageService minioStorageService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AvatarService avatarService;

    @BeforeEach
    void setUp() {
        avatarService = new AvatarService(avatarTaskMapper, minioStorageService, objectMapper);
    }

    @Test
    void deleteAvatar_shouldRemoveMinioObjectAndDeleteTasks() {
        String userId = "user_1";
        String avatarId = "avatar_123";
        String sourceUrl = "/uploads/avatars/user_1/avatars/avatar_123_source.png";

        AvatarTask task = new AvatarTask();
        task.setId(avatarId);
        task.setUserId(userId);
        task.setSourceImageUrl(sourceUrl);
        task.setDeleted(BizConstant.NOT_DELETED);

        when(avatarTaskMapper.selectById(avatarId)).thenReturn(task);
        when(minioStorageService.getBucketAvatars()).thenReturn("resumes-avatars");
        when(avatarTaskMapper.update(any(AvatarTask.class), any(LambdaQueryWrapper.class))).thenReturn(1);

        assertDoesNotThrow(() -> avatarService.deleteAvatar(userId, avatarId));

        verify(minioStorageService).remove("resumes-avatars", "user_1/avatars/avatar_123_source.png");
        verify(avatarTaskMapper).update(any(AvatarTask.class), any(LambdaQueryWrapper.class));

        ArgumentCaptor<AvatarTask> captor = ArgumentCaptor.forClass(AvatarTask.class);
        verify(avatarTaskMapper).update(captor.capture(), any(LambdaQueryWrapper.class));
        assertEquals(BizConstant.DELETED, captor.getValue().getDeleted());
    }

    @Test
    void deleteAvatar_shouldDenyWhenOwnerMismatch() {
        String userId = "user_1";
        String avatarId = "avatar_123";

        AvatarTask task = new AvatarTask();
        task.setId(avatarId);
        task.setUserId("user_2");
        task.setSourceImageUrl("/uploads/avatars/user_2/avatars/avatar_123_source.png");
        task.setDeleted(BizConstant.NOT_DELETED);

        when(avatarTaskMapper.selectById(avatarId)).thenReturn(task);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> avatarService.deleteAvatar(userId, avatarId));
        assertEquals(ResultCode.ACCESS_DENIED, ex.getErrorCode());
        verify(minioStorageService, never()).remove(anyString(), anyString());
    }

    @Test
    void optimizeAvatar_shouldTransitionFromPendingToSuccess() {
        OptimizeAvatarRequest request = new OptimizeAvatarRequest();
        request.setSourceImageUrl("/uploads/avatars/user_1/avatars/avatar_1_source.png");
        request.setResumeId("resume_1");
        request.setBackgroundType("blue");
        request.setStyle("formal");

        when(avatarTaskMapper.insert(any(AvatarTask.class))).thenAnswer(inv -> {
            AvatarTask task = inv.getArgument(0);
            task.setId("avatar_task_1");
            return 1;
        });
        when(avatarTaskMapper.updateById(any(AvatarTask.class))).thenReturn(1);

        Map<String, Object> result = avatarService.optimizeAvatar("user_1", request);

        String taskId = (String) result.get("taskId");
        assertNotNull(taskId);
        assertTrue(taskId.startsWith("avatar_task_"));
        assertEquals(BizConstant.TASK_STATUS_SUCCESS, result.get("status"));

        ArgumentCaptor<AvatarTask> captor = ArgumentCaptor.forClass(AvatarTask.class);
        verify(avatarTaskMapper, times(2)).updateById(captor.capture());
        AvatarTask finalTask = captor.getValue();
        assertEquals(BizConstant.TASK_STATUS_SUCCESS, finalTask.getStatus());
        assertNotNull(finalTask.getCompletedAt());
    }
}
