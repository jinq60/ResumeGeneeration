package com.resume.user.service;

import com.resume.common.constant.BizConstant;
import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.common.service.AuditLogService;
import com.resume.user.dto.*;
import com.resume.user.entity.RefreshToken;
import com.resume.user.entity.User;
import com.resume.user.entity.UserPreference;
import com.resume.user.mapper.RefreshTokenMapper;
import com.resume.user.mapper.UserMapper;
import com.resume.user.mapper.UserPreferenceMapper;
import com.resume.user.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import java.util.Map;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private RefreshTokenMapper refreshTokenMapper;

    @Mock
    private UserPreferenceMapper userPreferenceMapper;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private LoginAttemptGuard loginAttemptGuard;

    @Mock
    private AuditLogService auditLogService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userMapper, refreshTokenMapper, userPreferenceMapper, jwtTokenProvider,
                passwordEncoder, loginAttemptGuard, auditLogService);
        lenient().when(jwtTokenProvider.generateAccessToken(anyString(), anyBoolean())).thenReturn("access_token");
        lenient().when(jwtTokenProvider.generateAccessToken(anyString(), anyBoolean(), anyString())).thenReturn("access_token");
        lenient().when(jwtTokenProvider.generateRefreshToken(anyString(), any())).thenReturn("refresh_token");
        lenient().when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(3600000L);
        lenient().when(jwtTokenProvider.getRefreshTokenExpiration()).thenReturn(604800000L);
        lenient().when(refreshTokenMapper.insert(any(RefreshToken.class))).thenReturn(1);
        lenient().when(refreshTokenMapper.deleteById(anyString())).thenReturn(1);
    }

    @Test
    void login_shouldRejectUnknownEmailInsteadOfAutoCreate() {
        LoginRequest request = new LoginRequest();
        request.setAccount("newbie@example.com");
        request.setPassword("Passw0rd123");
        request.setLoginType("email");

        when(userMapper.selectOne(any())).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.login(request));
        assertEquals(ResultCode.AUTH_ACCOUNT_NOT_FOUND, ex.getErrorCode());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void login_shouldKeepRejectingUnknownPhone() {
        LoginRequest request = new LoginRequest();
        request.setAccount("13800000000");
        request.setPassword("Passw0rd123");
        request.setLoginType("phone");

        when(userMapper.selectOne(any())).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.login(request));
        assertEquals(ResultCode.AUTH_ACCOUNT_NOT_FOUND, ex.getErrorCode());
        verify(userMapper, never()).insert(any(User.class));
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

        UserService service = new UserService(userMapper, refreshTokenMapper, userPreferenceMapper, realProvider,
                passwordEncoder, loginAttemptGuard, auditLogService);
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
        stored.setFamilyId("family_1");
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
        // 轮换后的新令牌沿用同一家族 ID
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenMapper).insert(captor.capture());
        assertEquals("family_1", captor.getValue().getFamilyId());
    }

    @Test
    void refresh_shouldRevokeWholeFamilyOnReuse() {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("reused_token");

        when(jwtTokenProvider.validateRefreshToken("reused_token")).thenReturn(true);
        when(jwtTokenProvider.getUserId("reused_token")).thenReturn("user_1");

        RefreshToken stored = new RefreshToken();
        stored.setId("rt_1");
        stored.setUserId("user_1");
        stored.setFamilyId("family_1");
        stored.setExpiresAt(LocalDateTime.now().plusDays(1));
        when(refreshTokenMapper.selectOne(any())).thenReturn(stored);
        // 并发复用：先删后验影响 0 行 → 触发家族撤销
        when(refreshTokenMapper.delete(any())).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.refresh(request));
        assertEquals(ResultCode.AUTH_REFRESH_TOKEN_INVALID, ex.getErrorCode());
        // 第一次 delete 消费失败，第二次 delete 为家族撤销
        verify(refreshTokenMapper, times(2)).delete(any());
    }

    @Test
    void refresh_shouldRevokeWholeFamilyOnSequentialReplay() {
        // 失窃令牌在合法客户端刷新后被重放：签名合法但哈希查无记录（已被轮换消费）
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("stolen_old_token");

        when(jwtTokenProvider.validateRefreshToken("stolen_old_token")).thenReturn(true);
        when(jwtTokenProvider.getUserId("stolen_old_token")).thenReturn("user_1");
        when(jwtTokenProvider.getFamilyId("stolen_old_token")).thenReturn("family_1");
        when(refreshTokenMapper.selectOne(any())).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.refresh(request));
        assertEquals(ResultCode.AUTH_REFRESH_TOKEN_INVALID, ex.getErrorCode());
        // 必须按 claim 中的家族 ID 撤销整个家族，使被盗会话的新令牌一并失效
        verify(refreshTokenMapper).delete(any());
    }

    @Test
    void refresh_shouldRejectWithoutFamilyRevokeForLegacyToken() {
        // 历史令牌无 familyId claim：仅拒绝，不触发家族撤销
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("legacy_token");

        when(jwtTokenProvider.validateRefreshToken("legacy_token")).thenReturn(true);
        when(jwtTokenProvider.getUserId("legacy_token")).thenReturn("user_1");
        when(jwtTokenProvider.getFamilyId("legacy_token")).thenReturn(null);
        when(refreshTokenMapper.selectOne(any())).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.refresh(request));
        assertEquals(ResultCode.AUTH_REFRESH_TOKEN_INVALID, ex.getErrorCode());
        verify(refreshTokenMapper, never()).delete(any());
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

    @Test
    void updateProfile_shouldUpdateNicknameAndPhone() {
        User user = new User();
        user.setId("user_1");
        user.setIsGuest(0);
        when(userMapper.selectById("user_1")).thenReturn(user);
        when(userMapper.selectOne(any())).thenReturn(null);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setNickname("新昵称");
        request.setPhone("13800138000");

        UserInfoResponse response = userService.updateProfile("user_1", request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        assertEquals("新昵称", captor.getValue().getNickname());
        assertEquals("13800138000", captor.getValue().getPhone());
        assertNotNull(response);
    }

    @Test
    void updateProfile_shouldRejectDuplicatePhone() {
        User user = new User();
        user.setId("user_1");
        user.setIsGuest(0);
        when(userMapper.selectById("user_1")).thenReturn(user);

        User other = new User();
        other.setId("user_2");
        when(userMapper.selectOne(any())).thenReturn(other);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setPhone("13800138000");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.updateProfile("user_1", request));
        assertEquals(ResultCode.AUTH_PHONE_REGISTERED, ex.getErrorCode());
    }

    @Test
    void updateProfile_shouldRejectGuest() {
        User user = new User();
        user.setId("guest_1");
        user.setIsGuest(1);
        when(userMapper.selectById("guest_1")).thenReturn(user);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setNickname("昵称");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.updateProfile("guest_1", request));
        assertEquals(ResultCode.PARAM_INVALID, ex.getErrorCode());
    }

    @Test
    void getPreferences_shouldReturnStoredValues() {
        User user = new User();
        user.setId("user_1");
        when(userMapper.selectById("user_1")).thenReturn(user);

        UserPreference preference = new UserPreference();
        preference.setUserId("user_1");
        preference.setPreferences(Map.of("emailNotify", true));
        when(userPreferenceMapper.selectById("user_1")).thenReturn(preference);

        Map<String, Object> result = userService.getPreferences("user_1");

        assertEquals(Boolean.TRUE, result.get("emailNotify"));
    }

    @Test
    void savePreferences_shouldInsertWhenMissing() {
        User user = new User();
        user.setId("user_1");
        when(userMapper.selectById("user_1")).thenReturn(user);
        when(userPreferenceMapper.selectById("user_1")).thenReturn(null);

        Map<String, Object> result = userService.savePreferences("user_1", Map.of("emailNotify", false));

        ArgumentCaptor<UserPreference> captor = ArgumentCaptor.forClass(UserPreference.class);
        verify(userPreferenceMapper).insert(captor.capture());
        assertEquals(Boolean.FALSE, captor.getValue().getPreferences().get("emailNotify"));
        assertEquals(Map.of("emailNotify", false), result);
    }

    @Test
    void adminCreateUser_shouldGenerateTemporaryPassword() {
        when(userMapper.selectOne(any())).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId("user_new");
            return 1;
        });

        AdminCreateUserRequest request = new AdminCreateUserRequest();
        request.setEmail("new@example.com");
        request.setNickname("新用户");

        AdminCreateUserResponse response = userService.adminCreateUser(request);

        assertEquals("user_new", response.getUserId());
        assertNotNull(response.getTemporaryPassword());
        assertEquals(10, response.getTemporaryPassword().length());
    }

    @Test
    void adminCreateUser_shouldRejectDuplicateEmail() {
        User existing = new User();
        existing.setId("user_existing");
        existing.setEmail("dup@example.com");
        when(userMapper.selectOne(any())).thenReturn(existing);

        AdminCreateUserRequest request = new AdminCreateUserRequest();
        request.setEmail("dup@example.com");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.adminCreateUser(request));
        assertEquals(ResultCode.AUTH_EMAIL_REGISTERED, ex.getErrorCode());
    }
}
