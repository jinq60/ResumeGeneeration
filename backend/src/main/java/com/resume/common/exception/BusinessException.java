package com.resume.common.exception;

import lombok.Getter;

/**
 * 业务异常。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int errorCode;

    public BusinessException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(int errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public static BusinessException of(int errorCode, String message) {
        return new BusinessException(errorCode, message);
    }

    /**
     * 高频校验类异常可重写以避免填栈开销，业务侧按需使用匿名子类重写 fillInStackTrace。
     * 示例：throw new BusinessException(...){ @Override public synchronized Throwable fillInStackTrace(){return this;}};
     */
    @Override
    public synchronized Throwable fillInStackTrace() {
        return super.fillInStackTrace();
    }
}
