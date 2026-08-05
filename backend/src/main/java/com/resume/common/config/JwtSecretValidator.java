package com.resume.common.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * JWT secret 启动校验。
 * <p>
 * 仅生产 profile 要求 {@code app.jwt.secret} 显式来自环境变量，
 * 且 Base64 解码后长度 ≥ 256 bit（32 字节），否则启动即失败。
 * dev/test 环境由 {@code JwtTokenProvider} 自动生成临时 secret（与现有降级逻辑一致）。
 * </p>
 */
@Slf4j
@Component
@Profile("prod")
public class JwtSecretValidator {

    private static final int MIN_KEY_BYTES = 32;

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @PostConstruct
    public void validate() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                    "app.jwt.secret is required. Set JWT_SECRET env var to a Base64 encoded key "
                            + "of at least 256 bits (32 bytes).");
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(jwtSecret);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "app.jwt.secret is not a valid Base64 string. Set JWT_SECRET to a Base64 encoded key "
                            + "of at least 256 bits (32 bytes).", e);
        }
        if (decoded.length < MIN_KEY_BYTES) {
            throw new IllegalStateException(String.format(
                    "app.jwt.secret key length is %d bytes, must be at least %d bytes (256 bits).",
                    decoded.length, MIN_KEY_BYTES));
        }
        log.info("JWT secret validated: {} bytes", decoded.length);
    }
}