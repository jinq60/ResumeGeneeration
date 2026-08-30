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
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("请求参数不正确，请检查。");
        return ResponseEntity.badRequest().body(R.error(ResultCode.PARAM_INVALID, message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<R<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("IllegalArgument: {}", e.getMessage());
        return ResponseEntity.badRequest().body(R.error(ResultCode.PARAM_INVALID, e.getMessage() != null ? e.getMessage() : "请求参数不正确。"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<R<Void>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("Upload size exceeded: {}", e.getMessage());
        return ResponseEntity.badRequest().body(R.error(ResultCode.PARAM_INVALID, "上传文件过大，请检查大小限制。"));
    }

    @ExceptionHandler(org.springframework.web.multipart.MultipartException.class)
    public ResponseEntity<R<Void>> handleMultipartException(org.springframework.web.multipart.MultipartException e) {
        log.warn("Multipart error: {}", e.getMessage());
        return ResponseEntity.badRequest().body(R.error(ResultCode.PARAM_INVALID, "文件上传失败，请检查格式与大小。"));
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<R<Void>> handleAuthenticationException(org.springframework.security.core.AuthenticationException e) {
        log.warn("Authentication failed: {}", e.getMessage());
        return ResponseEntity.status(401).body(R.error(ResultCode.UNAUTHORIZED, "请先登录或登录已过期。"));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<R<Void>> handleMissingServletRequestPartException(MissingServletRequestPartException e) {
        return ResponseEntity.badRequest().body(R.error(ResultCode.PARAM_INVALID, "缺少请求部分: " + e.getRequestPartName()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<R<Void>> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(405).body(R.error(ResultCode.PARAM_INVALID, "不支持的请求方法: " + e.getMethod()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<R<Void>> handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
        String msg = e.getName() + ": 类型不正确，期望 " + (e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知");
        return ResponseEntity.badRequest().body(R.error(ResultCode.PARAM_INVALID, msg));
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

    /**
     * 唯一索引冲突兜底：按索引名映射到具体业务码，避免所有冲突都返回 TEMPLATE_CODE_EXISTS 误导前端。
     */
    @ExceptionHandler(org.springframework.dao.DuplicateKeyException.class)
    public ResponseEntity<R<Void>> handleDuplicateKeyException(org.springframework.dao.DuplicateKeyException e) {
        log.warn("Duplicate key conflict: {}", e.getMessage());
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        int code = ResultCode.TEMPLATE_CODE_EXISTS;
        String errMsg = "资源已存在或唯一性冲突，请检查后重试。";
        if (msg.contains("uk_user_phone")) {
            code = ResultCode.AUTH_PHONE_REGISTERED;
            errMsg = "该手机号已注册。";
        } else if (msg.contains("uk_user_email")) {
            code = ResultCode.AUTH_EMAIL_REGISTERED;
            errMsg = "该邮箱已注册。";
        } else if (msg.contains("uk_template_code")) {
            code = ResultCode.TEMPLATE_CODE_EXISTS;
            errMsg = "模板编码已存在。";
        } else if (msg.contains("uk_share_token") || msg.contains("uk_resume_share_token")) {
            code = ResultCode.TEMPLATE_CODE_EXISTS;
            errMsg = "分享已存在，请重试。";
        } else if (msg.contains("uk_ai_daily_quota")) {
            code = ResultCode.AI_DAILY_QUOTA_EXCEEDED;
            errMsg = "今日配额已达上限。";
        } else {
            log.warn("Unrecognized duplicate key, fallback to generic 409: {}", msg);
        }
        int httpStatus = resolveHttpStatus(code);
        return ResponseEntity.status(httpStatus).body(R.error(code, errMsg));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<R<Void>> handleNoResourceFoundException(NoResourceFoundException e) {
        return ResponseEntity.status(404).body(R.error(ResultCode.RESOURCE_NOT_FOUND, "请求的资源不存在。"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleException(Exception e) {
        log.error("Internal server error", e);
        return ResponseEntity.status(500).body(R.error(ResultCode.INTERNAL_ERROR, "服务器内部错误，请稍后再试。"));
    }

    private int resolveHttpStatus(int errorCode) {
        return switch (errorCode) {
            case ResultCode.PARAM_INVALID -> 400;
            case ResultCode.UNAUTHORIZED,
                 ResultCode.AUTH_ACCOUNT_NOT_FOUND,
                 ResultCode.AUTH_PASSWORD_INCORRECT,
                 ResultCode.AUTH_ACCOUNT_LOCKED,
                 ResultCode.AUTH_CREDENTIALS_INVALID,
                 ResultCode.AUTH_REFRESH_TOKEN_INVALID -> 401;
            case ResultCode.ACCESS_DENIED -> 403;
            case ResultCode.RESOURCE_NOT_FOUND,
                 ResultCode.RESUME_NOT_FOUND,
                 ResultCode.RESUME_TEMPLATE_NOT_FOUND,
                 ResultCode.TEMPLATE_NOT_FOUND,
                 ResultCode.AVATAR_SOURCE_NOT_FOUND,
                 ResultCode.AVATAR_TASK_NOT_FOUND,
                 ResultCode.PDF_TASK_NOT_FOUND,
                 ResultCode.AI_TASK_NOT_FOUND -> 404;
            case ResultCode.AUTH_PHONE_REGISTERED,
                 ResultCode.AUTH_EMAIL_REGISTERED,
                 ResultCode.TEMPLATE_CODE_EXISTS,
                 ResultCode.TEMPLATE_CODE_IMMUTABLE,
                 ResultCode.TEMPLATE_BUILTIN_PROTECTED,
                 ResultCode.RESUME_VERSION_CONFLICT -> 409;
            case ResultCode.IDEMPOTENCY_CONFLICT -> 425;
            case ResultCode.RATE_LIMITED,
                 ResultCode.AUTH_EMAIL_CODE_TOO_FREQUENT,
                 ResultCode.AUTH_SMS_CODE_TOO_FREQUENT,
                 ResultCode.AI_DAILY_QUOTA_EXCEEDED,
                 ResultCode.AI_CONCURRENT_LIMIT_EXCEEDED -> 429;
            case ResultCode.AUTH_EMAIL_CODE_SEND_FAILED -> 503;
            case ResultCode.PDF_EXPORT_FAILED -> 500;
            case ResultCode.INTERNAL_ERROR -> 500;
            // 参数/业务类错误（含验证码错误、密码强度不足等）统一按 400 返回
            default -> 400;
        };
    }
}
