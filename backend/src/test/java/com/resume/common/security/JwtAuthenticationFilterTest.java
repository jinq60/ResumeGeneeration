package com.resume.common.security;

import com.resume.common.constant.BizConstant;
import com.resume.user.security.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

/**
 * JWT 认证过滤器角色核对测试：
 * token claim 声明为 ADMIN 时回库核对实际角色，降权后旧 token 立即失去管理员权限。
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserAccountStatusProvider statusProvider;

    @Mock
    private ObjectProvider<UserAccountStatusProvider> statusProviderHolder;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtTokenProvider, statusProviderHolder);
        lenient().when(statusProviderHolder.getIfAvailable()).thenReturn(statusProvider);
        lenient().when(statusProvider.isEnabled(anyString())).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockToken(String userId, String role) {
        io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.claims()
                .subject(userId)
                .add(JwtTokenProvider.CLAIM_ROLE, role)
                .build();
        lenient().when(jwtTokenProvider.parseAccessClaims("token")).thenReturn(claims);
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        return request;
    }

    private List<String> currentAuthorities() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        return auth.getAuthorities().stream()
                .map(Object::toString)
                .collect(Collectors.toList());
    }

    @Test
    void shouldDowngradeAdminClaimWhenDatabaseRoleIsUser() throws Exception {
        // 管理员被降权后，旧 access token（claim 仍为 ADMIN）不得再持有管理员权限
        mockToken("user_1", BizConstant.USER_ROLE_ADMIN);
        when(statusProvider.findRole("user_1")).thenReturn(BizConstant.USER_ROLE_USER);

        filter.doFilterInternal(request(), new MockHttpServletResponse(), filterChain);

        List<String> authorities = currentAuthorities();
        assertTrue(authorities.contains("ROLE_USER"));
        assertFalse(authorities.contains("ROLE_ADMIN"));
    }

    @Test
    void shouldKeepAdminRoleWhenDatabaseRoleIsStillAdmin() throws Exception {
        mockToken("user_1", BizConstant.USER_ROLE_ADMIN);
        when(statusProvider.findRole("user_1")).thenReturn(BizConstant.USER_ROLE_ADMIN);

        filter.doFilterInternal(request(), new MockHttpServletResponse(), filterChain);

        List<String> authorities = currentAuthorities();
        assertTrue(authorities.contains("ROLE_ADMIN"));
        assertTrue(authorities.contains("ROLE_USER"));
    }

    @Test
    void shouldDowngradeToUserWhenRoleLookupReturnsNull() throws Exception {
        // DB 查无该用户（如已被删除）：旧 ADMIN token 不再以 USER 身份放行，直接视为未认证
        mockToken("user_1", BizConstant.USER_ROLE_ADMIN);
        when(statusProvider.findRole("user_1")).thenReturn(null);

        filter.doFilterInternal(request(), new MockHttpServletResponse(), filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldNotQueryRoleForNonAdminClaim() throws Exception {
        // 普通 USER claim 无需回库核对
        mockToken("user_1", BizConstant.USER_ROLE_USER);

        filter.doFilterInternal(request(), new MockHttpServletResponse(), filterChain);

        verify(statusProvider, never()).findRole(anyString());
        List<String> authorities = currentAuthorities();
        assertTrue(authorities.contains("ROLE_USER"));
        assertFalse(authorities.contains("ROLE_ADMIN"));
    }
}
