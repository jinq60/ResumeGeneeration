package com.resume.user.auth;

import com.resume.common.constant.ResultCode;
import com.resume.common.exception.BusinessException;
import com.resume.common.service.AuditLogService;
import com.resume.user.dto.AuthResponse;
import com.resume.user.entity.User;
import com.resume.user.entity.UserAuth;
import com.resume.user.mapper.UserAuthMapper;
import com.resume.user.mapper.UserMapper;
import com.resume.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserAuthServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserAuthMapper userAuthMapper;

    @Mock
    private UserService userService;

    @Mock
    private EmailCodeService emailCodeService;

    @Mock
    private SmsCodeService smsCodeService;

    @Mock
    private AuditLogService auditLogService;

    private UserAuthService service;

    @BeforeEach
    void setUp() {
        service = new UserAuthService(userMapper, userAuthMapper, userService,
                emailCodeService, smsCodeService, auditLogService);
    }

    private User buildUser(String id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setStatus("active");
        user.setDeleted(0);
        return user;
    }

    private void stubAuthResponse() {
        AuthResponse response = new AuthResponse();
        response.setUserId("user_1");
        response.setAccessToken("token_1");
        when(userService.buildAuthResponse(any())).thenReturn(response);
    }

    @Test
    void emailCodeLogin_shouldRejectInvalidEmail() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.authenticateByEmailCode("not-an-email", "123456"));
        assertEquals(ResultCode.PARAM_INVALID, ex.getErrorCode());
    }

    @Test
    void emailCodeLogin_shouldRejectWrongCode() {
        when(emailCodeService.verify("demo@example.com", "000000")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.authenticateByEmailCode("demo@example.com", "000000"));
        assertEquals(ResultCode.AUTH_EMAIL_CODE_INVALID, ex.getErrorCode());
    }

    @Test
    void emailCodeLogin_shouldCreateUserOnFirstLogin() {
        when(emailCodeService.verify("demo@example.com", "123456")).thenReturn(true);
        when(userMapper.selectOne(any())).thenReturn(null);
        stubAuthResponse();

        AuthResponse response = service.authenticateByEmailCode("demo@example.com", "123456");

        assertEquals("token_1", response.getAccessToken());
        verify(userMapper).insert(any(User.class));
    }

    @Test
    void emailCodeLogin_shouldReuseExistingUser() {
        when(emailCodeService.verify("demo@example.com", "123456")).thenReturn(true);
        when(userMapper.selectOne(any())).thenReturn(buildUser("user_exist", "demo@example.com"));
        stubAuthResponse();

        service.authenticateByEmailCode("demo@example.com", "123456");

        verify(userService).buildAuthResponse(any(User.class));
    }

    @Test
    void smsCodeLogin_shouldRejectInvalidPhone() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.authenticateBySmsCode("12345", "123456"));
        assertEquals(ResultCode.PARAM_INVALID, ex.getErrorCode());
    }

    @Test
    void smsCodeLogin_shouldRejectWrongCode() {
        when(smsCodeService.verify("13800138000", "000000")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.authenticateBySmsCode("13800138000", "000000"));
        assertEquals(ResultCode.AUTH_SMS_CODE_INVALID, ex.getErrorCode());
    }

    @Test
    void smsCodeLogin_shouldCreateUserOnFirstLogin() {
        when(smsCodeService.verify("13800138000", "123456")).thenReturn(true);
        when(userMapper.selectOne(any())).thenReturn(null);
        stubAuthResponse();

        AuthResponse response = service.authenticateBySmsCode("13800138000", "123456");

        assertEquals("token_1", response.getAccessToken());
        verify(userMapper).insert(any(User.class));
    }

    @Test
    void oauthLogin_shouldCreateAndBindNewUser() {
        when(userMapper.selectOne(any())).thenReturn(null);
        stubAuthResponse();

        AuthResponse response = service.authenticateByOAuth(
                new OAuthUserInfo("google", "sub_1", "oauth@example.com", "OAuth用户", null, true));

        assertEquals("token_1", response.getAccessToken());
        verify(userMapper).insert(any(User.class));
        verify(userAuthMapper).insert(any(UserAuth.class));
    }

    @Test
    void oauthLogin_shouldReuseExistingBinding() {
        UserAuth binding = new UserAuth();
        binding.setUserId("user_bound");
        when(userAuthMapper.selectOne(any())).thenReturn(binding);
        when(userMapper.selectById("user_bound")).thenReturn(buildUser("user_bound", "oauth@example.com"));
        stubAuthResponse();

        AuthResponse response = service.authenticateByOAuth(
                new OAuthUserInfo("github", "gh_1", null, null, null, false));

        assertEquals("token_1", response.getAccessToken());
    }

    @Test
    void oauthLogin_shouldRejectMissingAccountId() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.authenticateByOAuth(
                        new OAuthUserInfo("google", "  ", null, null, null, false)));
        assertEquals(ResultCode.AUTH_OAUTH_EXCHANGE_FAILED, ex.getErrorCode());
    }

    @Test
    void oauthLogin_shouldLinkExistingUserByEmail() {
        when(userAuthMapper.selectOne(any())).thenReturn(null);
        when(userMapper.selectOne(any())).thenReturn(buildUser("user_email", "oauth@example.com"));
        stubAuthResponse();

        service.authenticateByOAuth(
                new OAuthUserInfo("qq", "qq_1", "oauth@example.com", "QQ用户", null, true));

        verify(userAuthMapper).insert(any(UserAuth.class));
        verify(userService).buildAuthResponse(any(User.class));
    }
}
