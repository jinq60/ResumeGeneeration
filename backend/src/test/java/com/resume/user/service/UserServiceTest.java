package com.resume.user.service;

import com.resume.common.constant.BizConstant;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.user.dto.*;
import com.resume.user.entity.RefreshToken;
import com.resume.user.entity.User;
import com.resume.user.mapper.RefreshTokenMapper;
import com.resume.user.mapper.UserMapper;
import com.resume.user.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private RefreshTokenMapper refreshTokenMapper;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userMapper, refreshTokenMapper, jwtTokenProvider, passwordEncoder);
        lenient().when(jwtTokenProvider.generateAccessToken(anyString(), anyBoolean())).thenReturn("access_token");
        lenient().when(jwtTokenProvider.generateAccessToken(anyString(), anyBoolean(), anyString())).thenReturn("access_token");
        lenient().when(jwtTokenProvider.generateRefreshToken(anyString())).thenReturn("refresh_token");
        lenient().when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(3600000L);
        lenient().when(jwtTokenProvider.getRefreshTokenExpiration()).thenReturn(604800000L);
        lenient().when(refreshTokenMapper.insert(any(RefreshToken.class))).thenReturn(1);
        lenient().when(refreshTokenMapper.deleteById(anyString())).thenReturn(1);
    }

    @Test
    void register_shouldSucceedWithPhone() {
        RegisterRequest request = new RegisterRequest();
        request.setPhone("13800000000");
        request.setVerifyCode("123456");
        request.setPassword("Password123");

        when(userMapper.selectOne(any())).thenReturn(null);
        when(passwordEncoder.encode("Password123")).thenReturn("hashed");
        when(userMapper.insert(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId("user_1");
            return 1;
        });

        AuthResponse response = userService.register(request);

        assertEquals("user_1", response.getUserId());
        assertEquals("access_token", response.getAccessToken());
        assertFalse(response.getIsGuest());
        verify(userMapper).insert(any(User.class));
    }

    @Test
    void register_shouldRejectWeakPassword() {
        RegisterRequest request = new RegisterRequest();
        request.setPhone("13800000000");
        request.setVerifyCode("123456");
        request.setPassword("12");

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.register(request));
        assertEquals(ResultCode.AUTH_PASSWORD_TOO_WEAK, ex.getErrorCode());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void register_shouldRejectDuplicatePhone() {
        RegisterRequest request = new RegisterRequest();
        request.setPhone("13800000000");
        request.setVerifyCode("123456");
        request.setPassword("Password123");

        User exist = new User();
        exist.setId("user_old");
        exist.setDeleted(BizConstant.NOT_DELETED);
        when(userMapper.selectOne(argThat(wrapper -> true))).thenReturn(exist);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.register(request));
        assertEquals(ResultCode.AUTH_PHONE_REGISTERED, ex.getErrorCode());
    }

    @Test
    void login_shouldSucceedWithCorrectPassword() {
        LoginRequest request = new LoginRequest();
        request.setAccount("13800000000");
        request.setPassword("123456");
        request.setLoginType("phone");

        User user = new User();
        user.setId("user_1");
        user.setPhone("13800000000");
        user.setPasswordHash("hashed");
        user.setIsGuest(0);
        user.setStatus("active");

        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("123456", "hashed")).thenReturn(true);

        AuthResponse response = userService.login(request);
        assertEquals("user_1", response.getUserId());
    }

    @Test
    void login_shouldRejectWrongPassword() {
        LoginRequest request = new LoginRequest();
        request.setAccount("13800000000");
        request.setPassword("wrong");
        request.setLoginType("phone");

        User user = new User();
        user.setId("user_1");
        user.setPhone("13800000000");
        user.setPasswordHash("hashed");
        user.setIsGuest(0);
        user.setStatus("active");

        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.login(request));
        assertEquals(ResultCode.AUTH_PASSWORD_INCORRECT, ex.getErrorCode());
    }

    @Test
    void createGuest_shouldReturnGuestToken() {
        when(userMapper.insert(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId("guest_1");
            return 1;
        });

        AuthResponse response = userService.createGuest();

        assertEquals("guest_1", response.getUserId());
        assertTrue(response.getIsGuest());
        assertEquals("access_token", response.getAccessToken());
    }

    @Test
    void refresh_shouldRejectAccessToken() {
        // 使用真实的 JwtTokenProvider，确保 UserService.refresh 真正校验 refresh token 的类型/有效性，
        // 而不是通过 mock validateRefreshToken 来绕过校验逻辑。
        JwtTokenProvider realProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(realProvider, "jwtSecret",
                Base64.getEncoder().encodeToString("resume-generation-test-secret-key-must-be-is-32-bytes".getBytes()));
        ReflectionTestUtils.setField(realProvider, "accessTokenExpiration", 3600000L);
        ReflectionTestUtils.setField(realProvider, "refreshTokenExpiration", 604800000L);

        UserService service = new UserService(userMapper, refreshTokenMapper, realProvider, passwordEncoder);
        User user = new User();
        user.setId("user_1");
        user.setStatus(BizConstant.USER_STATUS_ACTIVE);
        when(userMapper.selectById("user_1")).thenReturn(user);

        String accessToken = realProvider.generateAccessToken("user_1", false);
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken(accessToken);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.refresh(request));
        assertEquals(ResultCode.AUTH_REFRESH_TOKEN_INVALID, ex.getErrorCode());
    }

    @Test
    void refresh_shouldRejectTokenNotInStore() {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("valid_but_not_stored");

        when(jwtTokenProvider.validateRefreshToken("valid_but_not_stored")).thenReturn(true);
        when(jwtTokenProvider.getUserId("valid_but_not_stored")).thenReturn("user_1");
        when(refreshTokenMapper.selectOne(any())).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.refresh(request));
        assertEquals(ResultCode.AUTH_REFRESH_TOKEN_INVALID, ex.getErrorCode());
    }

    @Test
    void refresh_shouldRejectExpiredStoredToken() {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("expired_in_store");

        when(jwtTokenProvider.validateRefreshToken("expired_in_store")).thenReturn(true);
        when(jwtTokenProvider.getUserId("expired_in_store")).thenReturn("user_1");

        RefreshToken stored = new RefreshToken();
        stored.setId("rt_1");
        stored.setUserId("user_1");
        stored.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(refreshTokenMapper.selectOne(any())).thenReturn(stored);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.refresh(request));
        assertEquals(ResultCode.AUTH_REFRESH_TOKEN_INVALID, ex.getErrorCode());
    }

    @Test
    void refresh_shouldRotateAndReturnNewTokens() {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("valid_stored_token");

        when(jwtTokenProvider.validateRefreshToken("valid_stored_token")).thenReturn(true);
        when(jwtTokenProvider.getUserId("valid_stored_token")).thenReturn("user_1");

        RefreshToken stored = new RefreshToken();
        stored.setId("rt_1");
        stored.setUserId("user_1");
        stored.setExpiresAt(LocalDateTime.now().plusDays(1));
        when(refreshTokenMapper.selectOne(any())).thenReturn(stored);

        User user = new User();
        user.setId("user_1");
        user.setIsGuest(0);
        user.setStatus("active");
        when(userMapper.selectById("user_1")).thenReturn(user);
        when(refreshTokenMapper.delete(any())).thenReturn(1);

        AuthResponse response = userService.refresh(request);

        assertEquals("user_1", response.getUserId());
        assertEquals("access_token", response.getAccessToken());
        assertEquals("refresh_token", response.getRefreshToken());
        verify(refreshTokenMapper).delete(any());
    }

    @Test
    void changePassword_shouldSucceedAndRevokeRefreshTokens() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("OldPass123");
        request.setNewPassword("NewPass456");

        User user = new User();
        user.setId("user_1");
        user.setPasswordHash("old_hashed");
        user.setIsGuest(0);
        user.setStatus("active");
        when(userMapper.selectById("user_1")).thenReturn(user);
        when(passwordEncoder.matches("OldPass123", "old_hashed")).thenReturn(true);
        when(passwordEncoder.encode("NewPass456")).thenReturn("new_hashed");

        userService.changePassword("user_1", request);

        verify(userMapper).updateById(argThat(u -> "new_hashed".equals(((User) u).getPasswordHash())));
        verify(refreshTokenMapper).delete(any());
    }

    @Test
    void changePassword_shouldRejectWrongOldPassword() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("WrongPass123");
        request.setNewPassword("NewPass456");

        User user = new User();
        user.setId("user_1");
        user.setPasswordHash("old_hashed");
        user.setIsGuest(0);
        when(userMapper.selectById("user_1")).thenReturn(user);
        when(passwordEncoder.matches("WrongPass123", "old_hashed")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.changePassword("user_1", request));
        assertEquals(ResultCode.AUTH_PASSWORD_INCORRECT, ex.getErrorCode());
        verify(userMapper, never()).updateById(any());
    }

    @Test
    void changePassword_shouldRejectWeakNewPassword() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("OldPass123");
        request.setNewPassword("123456");

        User user = new User();
        user.setId("user_1");
        user.setPasswordHash("old_hashed");
        user.setIsGuest(0);
        when(userMapper.selectById("user_1")).thenReturn(user);
        when(passwordEncoder.matches("OldPass123", "old_hashed")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.changePassword("user_1", request));
        assertEquals(ResultCode.AUTH_PASSWORD_TOO_WEAK, ex.getErrorCode());
        verify(userMapper, never()).updateById(any());
    }

    @Test
    void changePassword_shouldRejectGuest() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("OldPass123");
        request.setNewPassword("NewPass456");

        User user = new User();
        user.setId("guest_1");
        user.setIsGuest(1);
        when(userMapper.selectById("guest_1")).thenReturn(user);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.changePassword("guest_1", request));
        assertEquals(ResultCode.PARAM_INVALID, ex.getErrorCode());
    }
}
