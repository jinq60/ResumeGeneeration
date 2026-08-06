package com.resume.user.controller;

import com.resume.common.constant.ResultCode;
import com.resume.common.entity.R;
import com.resume.common.exception.BusinessException;
import com.resume.user.auth.AuthProvider;
import com.resume.user.auth.AuthProviderRegistry;
import com.resume.user.auth.EmailCodeService;
import com.resume.user.auth.OAuthProvider;
import com.resume.user.auth.OAuthStateStore;
import com.resume.user.auth.OAuthUserInfo;
import com.resume.user.auth.UserAuthService;
import com.resume.user.config.AuthProperties;
import com.resume.user.dto.*;
import com.resume.user.service.GuestAccountGuard;
import com.resume.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 认证相关接口（含多方式登录适配器）。
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final GuestAccountGuard guestAccountGuard;
    private final AuthProviderRegistry authProviderRegistry;
    private final EmailCodeService emailCodeService;
    private final OAuthStateStore oauthStateStore;
    private final UserAuthService userAuthService;
    private final AuthProperties authProperties;

    @PostMapping("/register")
    public R<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return R.success(userService.register(request));
    }

    /**
     * 账号密码登录（兼容旧客户端）。
     */
    @PostMapping("/login")
    public R<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return R.success(userService.login(request));
    }

    /**
     * 统一登录入口：按适配器方式认证（password / email_code / sms_code）。
     */
    @PostMapping("/login/{method}")
    public R<AuthResponse> loginByMethod(@PathVariable String method,
                                         @RequestBody Map<String, String> params) {
        AuthProvider provider = authProviderRegistry.getLoginProvider(method);
        if (!provider.isConfigured()) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "该登录方式暂未开放。");
        }
        return R.success(provider.authenticate(params == null ? Map.of() : params));
    }

    /**
     * 可用登录方式列表（前端渲染登录入口）。
     */
    @GetMapping("/methods")
    public R<Map<String, Object>> methods() {
        Map<String, Object> result = new HashMap<>();
        result.put("loginMethods", authProviderRegistry.allLoginProviders().stream()
                .map(p -> Map.of("method", p.method(), "configured", p.isConfigured()))
                .toList());
        result.put("oauthProviders", authProviderRegistry.allOAuthProviders().stream()
                .map(p -> Map.of("provider", p.provider(), "configured", p.isConfigured()))
                .toList());
        return R.success(result);
    }

    /**
     * 发送邮箱登录验证码。
     */
    @PostMapping("/email-code/send")
    public R<Void> sendEmailCode(@Valid @RequestBody SendEmailCodeRequest request) {
        emailCodeService.send(request.getEmail());
        return R.success();
    }

    /**
     * 邮箱验证码登录（免密，首次登录自动创建账号）。
     */
    @PostMapping("/email-code/login")
    public R<AuthResponse> emailCodeLogin(@Valid @RequestBody EmailCodeLoginRequest request) {
        return R.success(userAuthService.authenticateByEmailCode(request.getEmail(), request.getCode()));
    }

    /**
     * 第三方授权页跳转（浏览器直接访问）。
     */
    @GetMapping("/oauth/{provider}/authorize")
    public void authorize(@PathVariable String provider, HttpServletRequest request,
                          jakarta.servlet.http.HttpServletResponse response) throws IOException {
        OAuthProvider oauth = authProviderRegistry.getOAuthProvider(provider);
        if (!oauth.isConfigured()) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("第三方登录未配置，请联系管理员。");
            return;
        }
        String state = oauthStateStore.create(provider);
        response.sendRedirect(oauth.buildAuthorizeUrl(state));
    }

    /**
     * 第三方回调：换 token → 拉取用户 → 登录/绑定 → 302 回前端携带 JWT。
     */
    @GetMapping("/oauth/{provider}/callback")
    public void callback(@PathVariable String provider,
                         @RequestParam(required = false) String code,
                         @RequestParam(required = false) String state,
                         jakarta.servlet.http.HttpServletResponse response) throws IOException {
        String frontend = authProperties.getOauth().getFrontendRedirect();
        try {
            OAuthProvider oauth = authProviderRegistry.getOAuthProvider(provider);
            if (!oauthStateStore.consume(state, provider)) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "第三方登录状态校验失败，请重试。");
            }
            if (code == null || code.isBlank()) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "未获取到第三方授权码。");
            }
            OAuthUserInfo info = oauth.exchangeAndFetch(code);
            AuthResponse auth = userAuthService.authenticateByOAuth(info);
            response.sendRedirect(frontend + "?token=" + encode(auth.getAccessToken())
                    + "&refresh=" + encode(auth.getRefreshToken())
                    + "&guest=" + auth.getIsGuest());
        } catch (BusinessException e) {
            log.warn("OAuth callback failed: provider={}, error={}", provider, e.getMessage());
            response.sendRedirect(frontend + "?error=" + encode(e.getMessage()));
        } catch (Exception e) {
            log.error("OAuth callback error: provider={}", provider, e);
            response.sendRedirect(frontend + "?error=" + encode("第三方登录失败，请稍后重试。"));
        }
    }

    @PostMapping("/guest")
    public R<AuthResponse> guest(HttpServletRequest servletRequest) {
        // 每 IP 每日游客会话限额，防止刷号
        if (!guestAccountGuard.tryAcquire(resolveClientIp(servletRequest))) {
            throw new BusinessException(ResultCode.RATE_LIMITED, "游客体验名额已用完，请注册账号后使用。");
        }
        return R.success(userService.createGuest());
    }

    @PostMapping("/refresh")
    public R<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return R.success(userService.refresh(request));
    }

    @PostMapping("/logout")
    public R<Void> logout(@Valid @RequestBody LogoutRequest request) {
        userService.logout(request);
        return R.success();
    }

    /**
     * 获取客户端 IP：不信任 X-Forwarded-For，防止伪造绕过限额（与 RateLimitFilter 口径一致）。
     */
    private String resolveClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
