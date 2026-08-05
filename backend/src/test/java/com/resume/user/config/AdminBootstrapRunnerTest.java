package com.resume.user.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.resume.common.constant.BizConstant;
import com.resume.user.entity.User;
import com.resume.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminBootstrapRunnerTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminBootstrapRunner runner;

    @BeforeEach
    void setUp() {
        runner = new AdminBootstrapRunner(userMapper, passwordEncoder);
        ReflectionTestUtils.setField(runner, "bootstrapPhone", "");
        ReflectionTestUtils.setField(runner, "bootstrapPassword", "");
        ReflectionTestUtils.setField(runner, "bootstrapNickname", "管理员");
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
    }

    @Test
    void shouldSkipWhenNotConfigured() {
        runner.run(null);
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void shouldSkipWhenAdminExists() {
        ReflectionTestUtils.setField(runner, "bootstrapPhone", "13800000000");
        ReflectionTestUtils.setField(runner, "bootstrapPassword", "AdminPass123");
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        runner.run(null);

        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void shouldCreateFirstAdminWhenNoneExists() {
        ReflectionTestUtils.setField(runner, "bootstrapPhone", "13800000000");
        ReflectionTestUtils.setField(runner, "bootstrapPassword", "AdminPass123");
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(userMapper.insert(any(User.class))).thenAnswer(inv -> {
            ((User) inv.getArgument(0)).setId("admin_1");
            return 1;
        });

        runner.run(null);

        verify(userMapper).insert(argThat(u -> {
            User user = (User) u;
            return BizConstant.USER_ROLE_ADMIN.equals(user.getRole())
                    && "13800000000".equals(user.getPhone())
                    && "hashed".equals(user.getPasswordHash())
                    && BizConstant.USER_STATUS_ACTIVE.equals(user.getStatus());
        }));
    }

    @Test
    void shouldSkipOnInvalidInput() {
        ReflectionTestUtils.setField(runner, "bootstrapPhone", "not-a-phone");
        ReflectionTestUtils.setField(runner, "bootstrapPassword", "AdminPass123");
        runner.run(null);
        verify(userMapper, never()).insert(any(User.class));

        ReflectionTestUtils.setField(runner, "bootstrapPhone", "13800000000");
        ReflectionTestUtils.setField(runner, "bootstrapPassword", "weak");
        runner.run(null);
        verify(userMapper, never()).insert(any(User.class));
    }
}
