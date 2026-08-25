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
        if (StringUtils.hasText(token) && jwtTokenProvider.validateAccessToken(token)) {
            String userId = jwtTokenProvider.getUserId(token);
            // 账号被禁用/删除后旧 token 尽快失效
            UserAccountStatusProvider provider = statusProvider.getIfAvailable();
            if (provider == null || provider.isEnabled(userId)) {
                String role = resolveEffectiveRole(provider, userId, jwtTokenProvider.getRole(token));
                List<GrantedAuthority> authorities = buildAuthorities(role);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, token, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
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
        return actualRole != null ? actualRole : BizConstant.USER_ROLE_USER;
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
