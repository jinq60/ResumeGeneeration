package com.resume.user.security;

import com.resume.common.constant.BizConstant;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;

/**
 * JWT Token 生成与校验工具。
 * <p>
 * 访问令牌（access）与刷新令牌（refresh）通过 claim {@code type} 区分，防止刷新令牌被用于业务接口。
 * </p>
 */
@Slf4j
@Component
public class JwtTokenProvider {

    public static final String CLAIM_TYPE = "type";
    public static final String CLAIM_GUEST = "guest";
    public static final String CLAIM_ROLE = "role";

    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @Autowired(required = false)
    private Environment environment;

    @Value("${app.jwt.access-token-expiration:3600000}")
    private long accessTokenExpiration;

    @Value("${app.jwt.refresh-token-expiration:604800000}")
    private long refreshTokenExpiration;

    @PostConstruct
    public void initSecret() {
        if (!StringUtils.hasText(jwtSecret)) {
            if (isDevOrTestProfile()) {
                jwtSecret = generateRandomSecret();
                log.warn("JWT secret not configured for dev/test profile; generated a temporary secret.");
            } else {
                throw new IllegalStateException("app.jwt.secret must be configured for non-dev/test profiles.");
            }
        }
        // Validate the secret early (Base64-decodable and sufficient length).
        getSigningKey();
    }

    private boolean isDevOrTestProfile() {
        if (environment == null) {
            return false;
        }
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "dev".equals(profile) || "test".equals(profile));
    }

    private String generateRandomSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成访问令牌。
     */
    public String generateAccessToken(String userId, boolean guest) {
        return generateAccessToken(userId, guest, BizConstant.USER_ROLE_USER);
    }

    /**
     * 生成访问令牌（携带角色）。
     */
    public String generateAccessToken(String userId, boolean guest, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpiration);
        return Jwts.builder()
                .subject(userId)
                .claim(CLAIM_TYPE, TOKEN_TYPE_ACCESS)
                .claim(CLAIM_GUEST, guest)
                .claim(CLAIM_ROLE, role == null ? BizConstant.USER_ROLE_USER : role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 生成刷新令牌。
     */
    public String generateRefreshToken(String userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenExpiration);
        return Jwts.builder()
                .subject(userId)
                .claim(CLAIM_TYPE, TOKEN_TYPE_REFRESH)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 获取访问令牌过期时间（毫秒）。
     */
    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    /**
     * 获取刷新令牌过期时间（毫秒）。
     */
    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    /**
     * 从 Token 中解析用户 ID。
     */
    public String getUserId(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * 从 Token 中解析用户角色。
     */
    public String getRole(String token) {
        return parseClaims(token).get(CLAIM_ROLE, String.class);
    }

    /**
     * 从 Token 中解析类型。
     */
    public String getTokenType(String token) {
        return parseClaims(token).get(CLAIM_TYPE, String.class);
    }

    /**
     * 是否为访问令牌。
     */
    public boolean isAccessToken(String token) {
        return TOKEN_TYPE_ACCESS.equals(getTokenType(token));
    }

    /**
     * 是否为刷新令牌。
     */
    public boolean isRefreshToken(String token) {
        return TOKEN_TYPE_REFRESH.equals(getTokenType(token));
    }

    /**
     * 校验 Token 是否有效。
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 校验访问令牌是否有效。
     */
    public boolean validateAccessToken(String token) {
        return validateToken(token) && isAccessToken(token);
    }

    /**
     * 校验刷新令牌是否有效。
     */
    public boolean validateRefreshToken(String token) {
        return validateToken(token) && isRefreshToken(token);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
