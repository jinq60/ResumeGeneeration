package com.resume.common.handler;

import com.resume.common.constant.ResultCode;
import com.resume.common.entity.R;
import com.resume.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleBusinessException_shouldPreserveErrorCodeAndHttpStatus() {
        BusinessException ex = new BusinessException(ResultCode.RESUME_NOT_FOUND, "简历不存在。");
        ResponseEntity<R<Void>> response = handler.handleBusinessException(ex);

        assertEquals(404, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(ResultCode.RESUME_NOT_FOUND, response.getBody().getCode());
        assertEquals("简历不存在。", response.getBody().getMessage());
    }

    @Test
    void handleBusinessException_shouldMapConflictTo409() {
        BusinessException ex = new BusinessException(ResultCode.AUTH_PHONE_REGISTERED, "手机号已注册。");
        ResponseEntity<R<Void>> response = handler.handleBusinessException(ex);

        assertEquals(409, response.getStatusCode().value());
        assertEquals(ResultCode.AUTH_PHONE_REGISTERED, response.getBody().getCode());
    }

    @Test
    void handleBusinessException_shouldMapAuthModuleTo401() {
        BusinessException ex = new BusinessException(ResultCode.AUTH_PASSWORD_INCORRECT, "密码错误。");
        ResponseEntity<R<Void>> response = handler.handleBusinessException(ex);

        assertEquals(401, response.getStatusCode().value());
        assertEquals(ResultCode.AUTH_PASSWORD_INCORRECT, response.getBody().getCode());
    }
}
