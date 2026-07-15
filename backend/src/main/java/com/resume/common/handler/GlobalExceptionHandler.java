package com.resume.common.handler;

import com.resume.common.constant.ResultCode;
import com.resume.common.entity.R;
import com.resume.common.exception.BusinessException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<R<Void>> handleBusinessException(BusinessException e) {
        if (e.getCause() != null) {
            log.warn("Business exception: code={}, message={}", e.getErrorCode(), e.getMessage(), e);
        } else {
            log.warn("Business exception: code={}, message={}", e.getErrorCode(), e.getMessage());
        }
        int httpStatus = resolveHttpStatus(e.getErrorCode());
        return ResponseEntity.status(httpStatus).body(R.error(e.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<R<Void>> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("请求参数不正确，请检查。");
        return ResponseEntity.badRequest().body(R.error(ResultCode.PARAM_INVALID, message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<R<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(R.error(ResultCode.PARAM_INVALID, "请求体格式不正确，请检查 JSON。"));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<R<Void>> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .findFirst()
                .map(ConstraintViolation::getMessage)
                .orElse("请求参数不正确，请检查。");
        return ResponseEntity.badRequest().body(R.error(ResultCode.PARAM_INVALID, message));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<R<Void>> handleAccessDeniedException(AccessDeniedException e) {
        return ResponseEntity.status(403).body(R.error(ResultCode.ACCESS_DENIED, "无权访问该资源。"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleException(Exception e) {
        log.error("Internal server error", e);
        return ResponseEntity.status(500).body(R.error(ResultCode.INTERNAL_ERROR, "服务器内部错误，请稍后再试。"));
    }

    private int resolveHttpStatus(int errorCode) {
        return switch (errorCode) {
            case ResultCode.PARAM_INVALID -> 400;
            case ResultCode.UNAUTHORIZED -> 401;
            case ResultCode.ACCESS_DENIED -> 403;
            case ResultCode.RESOURCE_NOT_FOUND,
                 ResultCode.RESUME_NOT_FOUND,
                 ResultCode.TEMPLATE_NOT_FOUND,
                 ResultCode.AVATAR_TASK_NOT_FOUND,
                 ResultCode.PDF_TASK_NOT_FOUND -> 404;
            case ResultCode.AUTH_PHONE_REGISTERED,
                 ResultCode.AUTH_EMAIL_REGISTERED,
                 ResultCode.TEMPLATE_CODE_EXISTS,
                 ResultCode.TEMPLATE_CODE_IMMUTABLE,
                 ResultCode.TEMPLATE_BUILTIN_PROTECTED -> 409;
            case ResultCode.RATE_LIMITED -> 429;
            case ResultCode.INTERNAL_ERROR -> 500;
            default -> {
                if (errorCode >= 1000 && errorCode < 2000) {
                    yield 401;
                }
                if (errorCode >= 2000 && errorCode < 6000) {
                    yield 400;
                }
                yield 400;
            }
        };
    }
}
