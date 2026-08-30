package com.resume.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.common.constant.ResultCode;
import com.resume.common.entity.R;
import com.resume.common.mapper.IdempotencyRecordMapper;
import com.resume.common.security.IdempotencyFilter;
import com.resume.common.security.JwtAuthenticationEntryPoint;
import com.resume.common.security.JwtAuthenticationFilter;
import com.resume.common.security.RateLimitFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * 安全基础配置：无会话、JWT 认证、CORS、RBAC。
 * <p>
 * 过滤器均通过 filterChain 方法参数注入，避免与 @Bean 方法构成构造器循环依赖。
 * </p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    /**
     * bcrypt cost=12：相较默认 10 显著提升离线爆破成本；
     * 哈希自带 cost 参数，存量 cost=10 哈希仍可正常校验，无需迁移。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public IdempotencyFilter idempotencyFilter(IdempotencyRecordMapper idempotencyRecordMapper,
                                               ObjectMapper objectMapper) {
        return new IdempotencyFilter(idempotencyRecordMapper, objectMapper);
    }

    /**
     * 关闭 Spring Boot 对安全过滤器的自动注册，避免其既作为全局 Servlet Filter 执行、
     * 又被加入 Security 链导致双重注册（OncePerRequestFilter 虽防重复执行，但实际执行
     * 顺序会依赖容器 filter order，使幂等过滤器可能在 JWT 认证之前执行）。
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter filter) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<IdempotencyFilter> idempotencyFilterRegistration(IdempotencyFilter filter) {
        FilterRegistrationBean<IdempotencyFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationFilter jwtAuthenticationFilter,
                                           JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                                           RateLimitFilter rateLimitFilter,
                                           IdempotencyFilter idempotencyFilter,
                                           ObjectMapper objectMapper) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                // 授权失败（如 USER 访问 /admin/**）返回统一 R<T> JSON，而非 Spring 默认空 403
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(403);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    response.getWriter().write(objectMapper.writeValueAsString(
                            R.error(ResultCode.ACCESS_DENIED, "无权访问该资源。")));
                }))
            // 链内顺序：JWT 认证 → 限流 → 幂等 → UsernamePasswordAuthenticationFilter。
            // JWT 必须先于限流执行，使限流能按已认证的 userId 计数（见 RateLimitFilter.resolveKey）。
            // 注意：addFilterBefore 多次以同一锚点插入时，后插入者更靠近锚点（更先执行），
            // 故不能连续三次 addFilterBefore(..., UsernamePassword)，否则实际顺序为 Idempotency→RateLimit→Jwt。
            // 此处采用 addFilterBefore + addFilterAfter 保证声明顺序即执行顺序。
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(rateLimitFilter, JwtAuthenticationFilter.class)
            .addFilterAfter(idempotencyFilter, RateLimitFilter.class)
            // 全局安全头（API 与静态资源均生效，StaticResourceController 另有细化 CSP）
            .headers(headers -> headers
                    .frameOptions(frame -> frame.deny())
                    .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
                    .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/templates", "/templates/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/share/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/uploads/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        if (origins.contains("*") && origins.size() == 1) {
            throw new IllegalStateException("CORS allowedOrigins cannot be '*' when allowCredentials is true");
        }
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Idempotency-Key", "X-Trace-Id"));
        configuration.setExposedHeaders(List.of("Content-Disposition"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
