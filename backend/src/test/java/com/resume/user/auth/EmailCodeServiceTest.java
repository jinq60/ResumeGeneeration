package com.resume.user.auth;

import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.user.config.AuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class EmailCodeServiceTest {

    private EmailCodeService service;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        AuthProperties properties = new AuthProperties();
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        service = new EmailCodeService(properties, provider);
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

    @SuppressWarnings("unchecked")
    private String extractCode(String email) throws Exception {
        java.lang.reflect.Field field = EmailCodeService.class.getDeclaredField("codes");
        field.setAccessible(true);
        Object entry = ((java.util.Map<String, Object>) field.get(service)).get(email.toLowerCase());
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
}
