package com.resume.avatar.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.ai.service.AiAvatarService;
import com.resume.avatar.dto.OptimizeAvatarRequest;
import com.resume.avatar.entity.AvatarTask;
import com.resume.avatar.mapper.AvatarTaskMapper;
import com.resume.common.constant.BizConstant;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.common.service.MinioStorageService;
import com.resume.resume.service.ResumeService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AvatarServiceTest {

    @Mock
    private AvatarTaskMapper avatarTaskMapper;

    @Mock
    private MinioStorageService minioStorageService;

    @Mock
    private ResumeService resumeService;

    @Mock
    private AiAvatarService aiAvatarService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AvatarService avatarService;

    @BeforeAll
    static void initTableInfo() {
        // LambdaUpdateWrapper.set() 解析列名需要实体的 TableInfo 缓存（单测无 MyBatis 环境，手动初始化）
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), AvatarTask.class);
    }

    @BeforeEach
    void setUp() {
        avatarService = new AvatarService(avatarTaskMapper, minioStorageService, objectMapper,
                resumeService, aiAvatarService);
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
        when(avatarTaskMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        assertDoesNotThrow(() -> avatarService.deleteAvatar(userId, avatarId));

        // 文件删除延迟到事务提交后执行（本测试无事务，removeAfterCommit 内部立即删除）
        verify(minioStorageService).removeAfterCommit("resumes-avatars",
                java.util.List.of("user_1/avatars/avatar_123_source.png"));
        // 逻辑删除必须通过 UpdateWrapper.set 显式写入 deleted 列（实体方式会被 MP 排除在 SET 外）
        ArgumentCaptor<LambdaUpdateWrapper> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(avatarTaskMapper).update(isNull(), captor.capture());
        String sqlSet = captor.getValue().getSqlSet();
        assertTrue(sqlSet != null && sqlSet.contains("deleted"),
                "UPDATE 语句的 SET 子句必须包含 deleted 列");
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
    void optimizeAvatar_shouldCreatePendingTaskAndSubmitAsync() {
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

        Map<String, Object> result = avatarService.optimizeAvatar("user_1", request);

        String taskId = (String) result.get("taskId");
        assertNotNull(taskId);
        assertEquals(BizConstant.TASK_STATUS_PENDING, result.get("status"));

        // 异步任务已提交
        verify(aiAvatarService).executeOptimize("avatar_task_1");

        ArgumentCaptor<AvatarTask> captor = ArgumentCaptor.forClass(AvatarTask.class);
        verify(avatarTaskMapper).insert(captor.capture());
        assertEquals(BizConstant.TASK_STATUS_PENDING, captor.getValue().getStatus());
    }
}
