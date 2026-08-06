package com.resume.user.auth.provider;

import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.user.auth.AuthMethod;
import com.resume.user.auth.AuthProvider;
import com.resume.user.dto.AuthResponse;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 手机验证码登录适配器（预留）。
 * <p>
 * 短信通道接入后实现本方法，当前统一返回“未开放”错误。
 * </p>
 */
@Component
public class SmsCodeAuthProvider implements AuthProvider {

    @Override
    public String method() {
        return AuthMethod.SMS_CODE;
    }

    @Override
    public AuthResponse authenticate(Map<String, String> params) {
        throw new BusinessException(ResultCode.AUTH_SMS_CODE_NOT_AVAILABLE,
                "手机验证码登录即将上线，请先使用其他方式登录。");
    }

    @Override
    public boolean isConfigured() {
        return false;
    }
}
