package com.resume.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 占位验证码校验（仅限开发/测试环境）。
 * <p>
 * 激活条件：{@code app.auth.verify-code.mode=placeholder}（默认）。
 * 生产环境（prod profile 显式配置 strict）不会激活本实现。
 * </p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.auth.verify-code.mode", havingValue = "placeholder", matchIfMissing = true)
public class PlaceholderVerifyCodeService implements VerifyCodeService {

    @Override
    public boolean verify(String code, String target) {
        // P0 占位：任意 6 位数字均通过
        return code != null && code.matches("^\\d{6}$");
    }

    @Override
    public String mode() {
        return "placeholder";
    }
}
