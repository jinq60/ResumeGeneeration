package com.resume.user.auth;

import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.user.config.AuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmailCodeServiceTest {

    private EmailCodeService service;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        AuthProperties properties = new AuthProperties();
        ObjectProvider<JavaMailSender> mailProvider = mock(ObjectProvider.class);
        // 未配置 Redis：走内存模式
        ObjectProvider<RedisTemplate<String, String>> redisProvider = mock(ObjectProvider.class);
        // dev profile：未配置 SMTP 时降级为日志输出验证码（本地联调）
        ObjectProvider<org.springframework.core.env.Environment> envProvider = mock(ObjectProvider.class);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");
        when(envProvider.getIfAvailable()).thenReturn(environment);
        service = new EmailCodeService(properties, mailProvider, redisProvider, envProvider);
    }

    @Test
    void send_shouldAllowRepeatedSendAfterInterval() {
        service.send("demo@example.com");
        // 间隔内重发应被拒绝
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.send("demo@example.com"));
        assertEquals(ResultCode.AUTH_EMAIL_CODE_TOO_FREQUENT, ex.getErrorCode());
    }

    @Test
    void verify_shouldAcceptCodeSentViaSend() throws Exception {
        service.send("demo@example.com");
        String code = extractCode("demo@example.com");
        assertTrue(service.verify("demo@example.com", code));
    }

    @Test
    void verify_shouldBeSingleUse() throws Exception {
        service.send("single@example.com");
        String code = extractCode("single@example.com");
        assertTrue(service.verify("single@example.com", code));
        assertFalse(service.verify("single@example.com", code));
    }

    @Test
    void verify_shouldRejectWrongCode() throws Exception {
        service.send("wrong@example.com");
        assertFalse(service.verify("wrong@example.com", "000000"));
    }

    @Test
    void verify_shouldInvalidateAfterMaxAttempts() throws Exception {
        service.send("brute@example.com");
        String code = extractCode("brute@example.com");
        // 连续 5 次错误后验证码作废，正确码也无法通过
        for (int i = 0; i < 5; i++) {
            assertFalse(service.verify("brute@example.com", "000000"));
        }
        assertFalse(service.verify("brute@example.com", code));
    }

    @Test
    void verify_shouldKeepWorkingAfterFewWrongAttempts() throws Exception {
        service.send("ok@example.com");
        String code = extractCode("ok@example.com");
        assertFalse(service.verify("ok@example.com", "111111"));
        assertFalse(service.verify("ok@example.com", "222222"));
        // 未超过上限，正确码仍可登录
        assertTrue(service.verify("ok@example.com", code));
    }

    @SuppressWarnings("unchecked")
    private String extractCode(String email) throws Exception {
        java.lang.reflect.Field field = EmailCodeService.class.getDeclaredField("codes");
        field.setAccessible(true);
        com.github.benmanes.caffeine.cache.Cache<String, Object> cache =
                (com.github.benmanes.caffeine.cache.Cache<String, Object>) field.get(service);
        Object entry = cache.getIfPresent(email.toLowerCase());
        assertNotNull(entry);
        return (String) entry.getClass().getDeclaredMethod("code").invoke(entry);
    }

    @Test
    void send_shouldBeCaseInsensitive() {
        service.send("Demo@Example.com");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.send("demo@example.com"));
        assertEquals(ResultCode.AUTH_EMAIL_CODE_TOO_FREQUENT, ex.getErrorCode());
    }

    @Test
    @SuppressWarnings("unchecked")
    void send_shouldFailFastInProdWhenSmtpNotConfigured() throws Exception {
        // 生产 profile 未配置 SMTP：必须 fail-fast，且验证码不得落库/泄露到日志
        AuthProperties properties = new AuthProperties();
        ObjectProvider<JavaMailSender> mailProvider = mock(ObjectProvider.class);
        ObjectProvider<RedisTemplate<String, String>> redisProvider = mock(ObjectProvider.class);
        ObjectProvider<org.springframework.core.env.Environment> envProvider = mock(ObjectProvider.class);
        MockEnvironment prodEnvironment = new MockEnvironment();
        prodEnvironment.setActiveProfiles("prod");
        when(envProvider.getIfAvailable()).thenReturn(prodEnvironment);

        EmailCodeService prodService = new EmailCodeService(properties, mailProvider, redisProvider, envProvider);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> prodService.send("prod@example.com"));
        assertEquals(ResultCode.AUTH_EMAIL_CODE_SEND_FAILED, ex.getErrorCode());

        java.lang.reflect.Field field = EmailCodeService.class.getDeclaredField("codes");
        field.setAccessible(true);
        Object cache = field.get(prodService);
        @SuppressWarnings("unchecked")
        com.github.benmanes.caffeine.cache.Cache<?, ?> c = (com.github.benmanes.caffeine.cache.Cache<?, ?>) cache;
        assertTrue(c.asMap().isEmpty(),
                "生产环境发送失败时不应落库任何验证码");
    }
}
