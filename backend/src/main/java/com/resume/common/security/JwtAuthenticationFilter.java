package com.resume.common.security;

import com.resume.common.constant.BizConstant;
import com.resume.user.security.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JWT 认证过滤器：从请求头解析访问令牌并写入 SecurityContext。
 * <p>
 * 仅 {@code type=access} 的令牌可用于业务接口；刷新令牌会被忽略，从而防止 7 天刷新令牌直接访问业务接口。
 * </p>
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final org.springframework.beans.factory.ObjectProvider<UserAccountStatusProvider> statusProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (StringUtils.hasText(token)) {
            try {
                io.jsonwebtoken.Claims claims = jwtTokenProvider.parseAccessClaims(token);
                String userId = claims.getSubject();
                String claimedRole = claims.get(com.resume.user.security.JwtTokenProvider.CLAIM_ROLE, String.class);
                // 账号被禁用/删除后旧 token 尽快失效；provider 为 null 仅发生在单测 slice（无 user 模块），此时放行以便测试
                UserAccountStatusProvider provider = statusProvider.getIfAvailable();
                if (provider != null && !provider.isEnabled(userId)) {
                    filterChain.doFilter(request, response);
                    return;
                }
                String role = resolveEffectiveRole(provider, userId, claimedRole);
                if (role == null) {
                    // 用户不存在或已被删除（ADMIN token 降级时实际角色为 null），视为未认证
                    filterChain.doFilter(request, response);
                    return;
                }
                List<GrantedAuthority> authorities = buildAuthorities(role);
                // credentials 置空，避免 token 通过 SecurityContext 序列化/日志泄露
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
                // 无效 token 静默放行，由 EntryPoint 处理 401
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 解析实际生效角色：token claim 声明为 ADMIN 时回库核对当前角色，
     * 防止管理员被降权后旧 access token（最长 1 小时有效）仍持有管理员权限。
     * <ul>
     *   <li>DB 角色仍为 ADMIN → 维持 ADMIN；</li>
     *   <li>DB 角色为其他值（已降权）→ 按 DB 实际角色授权；</li>
     *   <li>DB 无记录 / 状态提供者不可用 → 保守降级为普通用户。</li>
     * </ul>
     */
    private String resolveEffectiveRole(UserAccountStatusProvider provider, String userId, String claimedRole) {
        if (!BizConstant.USER_ROLE_ADMIN.equals(claimedRole)) {
            return claimedRole;
        }
        if (provider == null) {
            return BizConstant.USER_ROLE_USER;
        }
        String actualRole = provider.findRole(userId);
        // 用户不存在（已被删除）时，旧 ADMIN token 不再以 USER 身份放行
        return actualRole;
    }

    private List<GrantedAuthority> buildAuthorities(String role) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (BizConstant.USER_ROLE_ADMIN.equals(role)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + BizConstant.USER_ROLE_ADMIN));
        }
        authorities.add(new SimpleGrantedAuthority("ROLE_" + BizConstant.USER_ROLE_USER));
        return authorities;
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
