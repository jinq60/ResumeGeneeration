package com.resume.user.auth;

import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.user.config.AuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 短信验证码服务安全测试：
 * 未配置短信通道时生产环境拒绝发送（半开放修复），dev/test 保持联调可用；
 * 每手机号每日发送上限防刷。
 */
class SmsCodeServiceTest {

    private AuthProperties properties;
    private MockEnvironment environment;
    private SmsCodeService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        properties = new AuthProperties();
        // 关闭重发间隔，便于连续发送测试每日上限
        properties.getEmailCode().setResendIntervalSeconds(0);

        ObjectProvider<RedisTemplate<String, String>> redisProvider = mock(ObjectProvider.class);
        when(redisProvider.getIfAvailable()).thenReturn(null); // 内存模式

        environment = new MockEnvironment();
        service = new SmsCodeService(properties, redisProvider);
        ReflectionTestUtils.setField(service, "environment", environment);
    }

    private void setChannelConfig(String apiKey, int dailyLimit) {
        ReflectionTestUtils.setField(service, "smsApiKey", apiKey);
        ReflectionTestUtils.setField(service, "dailyLimitPerPhone", dailyLimit);
    }

    @Test
    void isConfigured_shouldDependOnApiKey() {
        setChannelConfig("", 10);
        assertFalse(service.isConfigured());
        setChannelConfig("real-key", 10);
        assertTrue(service.isConfigured());
    }

    @Test
    void send_shouldRejectInProdWhenChannelNotConfigured() {
        // 半开放防线：生产环境未接入真实通道时直接拒绝，不生成/不落库验证码
        environment.setActiveProfiles("prod");
        setChannelConfig("", 10);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.send("13800138000"));
        assertEquals(ResultCode.AUTH_SMS_CODE_NOT_AVAILABLE, ex.getErrorCode());

        // 不落 Redis / 内存
        com.github.benmanes.caffeine.cache.Cache<?, ?> codes =
                (com.github.benmanes.caffeine.cache.Cache<?, ?>) ReflectionTestUtils.getField(service, "codes");
        assertTrue(codes.asMap().isEmpty());
    }

    @Test
    void send_shouldStillWorkInDevWithoutChannel() {
        // dev/test profile 保持现状：打印验证码方便联调
        environment.setActiveProfiles("dev");
        setChannelConfig("", 10);

        assertDoesNotThrow(() -> service.send("13800138000"));

        @SuppressWarnings("unchecked")
        com.github.benmanes.caffeine.cache.Cache<String, ?> codes =
                (com.github.benmanes.caffeine.cache.Cache<String, ?>) ReflectionTestUtils.getField(service, "codes");
        assertNotNull(codes.getIfPresent("13800138000"));
    }

    @Test
    void send_shouldRejectWhenDailyLimitExceeded() {
        environment.setActiveProfiles("dev");
        setChannelConfig("real-key", 2);

        assertDoesNotThrow(() -> service.send("13800138000"));
        assertDoesNotThrow(() -> service.send("13800138000"));
        // 第 3 次超过每日上限（默认 10 条，此处配置为 2）
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.send("13800138000"));
        assertEquals(ResultCode.RATE_LIMITED, ex.getErrorCode());
    }

    @Test
    void send_dailyLimitShouldBePerPhone() {
        environment.setActiveProfiles("dev");
        setChannelConfig("real-key", 1);

        assertDoesNotThrow(() -> service.send("13800138000"));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.send("13800138000"));
        assertEquals(ResultCode.RATE_LIMITED, ex.getErrorCode());
        // 其他手机号不受影响
        assertDoesNotThrow(() -> service.send("13900139000"));
    }
}
