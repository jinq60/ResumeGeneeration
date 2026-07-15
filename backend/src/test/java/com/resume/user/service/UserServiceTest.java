package com.resume.user.service;

import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.user.dto.*;
import com.resume.user.entity.User;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userMapper, jwtTokenProvider, passwordEncoder);
        lenient().when(jwtTokenProvider.generateAccessToken(anyString(), anyBoolean())).thenReturn("access_token");
        lenient().when(jwtTokenProvider.generateAccessToken(anyString(), anyBoolean(), anyString())).thenReturn("access_token");
        lenient().when(jwtTokenProvider.generateRefreshToken(anyString())).thenReturn("refresh_token");
        lenient().when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(3600000L);
    }

    @Test
    void register_shouldSucceedWithPhone() {
        RegisterRequest request = new RegisterRequest();
        request.setPhone("13800000000");
        request.setVerifyCode("123456");
        request.setPassword("123456");

        when(userMapper.selectOne(any())).thenReturn(null);
        when(passwordEncoder.encode("123456")).thenReturn("hashed");
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
        request.setPassword("123456");

        User exist = new User();
        exist.setId("user_old");
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
    void refresh_shouldRejectInvalidToken() {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("invalid_token");

        when(jwtTokenProvider.validateToken("invalid_token")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.refresh(request));
        assertEquals(ResultCode.AUTH_REFRESH_TOKEN_INVALID, ex.getErrorCode());
    }
}
