package com.resume.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.common.entity.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 未认证请求入口点，统一返回 401 标准响应。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.warn("Unauthorized request: {} error={}", request.getRequestURI(), authException.getMessage());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader("WWW-Authenticate", "Bearer error=\"invalid_token\", error_description=\"请先登录后再访问\"");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        R<Void> body = R.error(com.resume.common.constant.ResultCode.UNAUTHORIZED, "请先登录后再访问。");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
