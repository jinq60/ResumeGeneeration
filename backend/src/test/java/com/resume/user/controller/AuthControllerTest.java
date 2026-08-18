package com.resume.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.common.mapper.IdempotencyRecordMapper;
import com.resume.common.service.RateLimiter;
import com.resume.user.auth.AuthProviderRegistry;
import com.resume.user.auth.EmailCodeService;
import com.resume.user.auth.OAuthProvider;
import com.resume.user.auth.OAuthStateStore;
import com.resume.user.auth.OAuthUserInfo;
import com.resume.user.auth.SmsCodeService;
import com.resume.user.auth.UserAuthService;
import com.resume.user.config.AuthProperties;
import com.resume.user.dto.AuthResponse;
import com.resume.user.security.JwtTokenProvider;
import com.resume.user.dto.LoginRequest;
import com.resume.user.service.GuestAccountGuard;
import com.resume.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(AuthProperties.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private GuestAccountGuard guestAccountGuard;

    @MockBean
    private AuthProviderRegistry authProviderRegistry;

    @MockBean
    private EmailCodeService emailCodeService;

    @MockBean
    private SmsCodeService smsCodeService;

    @MockBean
    private OAuthStateStore oauthStateStore;

    @MockBean
    private UserAuthService userAuthService;

    @MockBean
    private IdempotencyRecordMapper idempotencyRecordMapper;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private RateLimiter rateLimiter;

    private AuthResponse buildAuthResponse() {
        AuthResponse response = new AuthResponse();
        response.setUserId("user123");
        response.setAccessToken("token123");
        response.setRefreshToken("refresh123");
        response.setIsGuest(false);
        return response;
    }

    @Test
    void testLogin_Success() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setAccount("13800138000");
        request.setPassword("Password123");
        request.setLoginType("phone");

        when(userService.login(any(LoginRequest.class))).thenReturn(buildAuthResponse());

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value("user123"))
                .andExpect(jsonPath("$.data.accessToken").value("token123"));
    }

    @Test
    void testGuest_Success() throws Exception {
        AuthResponse response = new AuthResponse();
        response.setUserId("guest123");
        response.setAccessToken("guestToken123");
        response.setIsGuest(true);

        when(userService.createGuest()).thenReturn(response);
        when(guestAccountGuard.tryAcquire(any())).thenReturn(true);

        mockMvc.perform(post("/auth/guest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value("guest123"))
                .andExpect(jsonPath("$.data.isGuest").value(true));
    }

    @Test
    void testSendEmailCode_Success() throws Exception {
        mockMvc.perform(post("/auth/email-code/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"demo@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testSendEmailCode_InvalidEmail() throws Exception {
        mockMvc.perform(post("/auth/email-code/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testEmailCodeLogin_Success() throws Exception {
        when(userAuthService.authenticateByEmailCode(eq("demo@example.com"), eq("123456")))
                .thenReturn(buildAuthResponse());

        mockMvc.perform(post("/auth/email-code/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"demo@example.com\",\"code\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("token123"));
    }

    @Test
    void testEmailCodeLogin_BadCodeFormat() throws Exception {
        mockMvc.perform(post("/auth/email-code/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"demo@example.com\",\"code\":\"abc\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testOAuthAuthorize_NotConfigured() throws Exception {
        OAuthProvider oauth = mock(OAuthProvider.class);
        when(authProviderRegistry.getOAuthProvider("google")).thenReturn(oauth);
        when(oauth.isConfigured()).thenReturn(false);

        mockMvc.perform(get("/auth/oauth/google/authorize"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testOAuthAuthorize_RedirectsToProvider() throws Exception {
        OAuthProvider oauth = mock(OAuthProvider.class);
        when(authProviderRegistry.getOAuthProvider("github")).thenReturn(oauth);
        when(oauth.isConfigured()).thenReturn(true);
        when(oauthStateStore.create("github")).thenReturn("state123");
        when(oauth.buildAuthorizeUrl("state123"))
                .thenReturn("https://github.com/login/oauth/authorize?client_id=x&state=state123");

        mockMvc.perform(get("/auth/oauth/github/authorize"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location",
                        "https://github.com/login/oauth/authorize?client_id=x&state=state123"));
    }

    @Test
    void testOAuthCallback_Success() throws Exception {
        OAuthProvider oauth = mock(OAuthProvider.class);
        when(authProviderRegistry.getOAuthProvider("google")).thenReturn(oauth);
        when(oauthStateStore.consume("state123", "google")).thenReturn(true);
        when(oauth.exchangeAndFetch("code123"))
                .thenReturn(new OAuthUserInfo("google", "sub_1", "demo@example.com", "Demo", null, true));
        when(userAuthService.authenticateByOAuth(any())).thenReturn(buildAuthResponse());

        mockMvc.perform(get("/auth/oauth/google/callback")
                .param("code", "code123")
                .param("state", "state123"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.containsString("/login?token=token123")));
    }

    @Test
    void testOAuthCallback_BadState() throws Exception {
        when(authProviderRegistry.getOAuthProvider("google"))
                .thenReturn(mock(OAuthProvider.class));
        when(oauthStateStore.consume("bad", "google")).thenReturn(false);

        mockMvc.perform(get("/auth/oauth/google/callback")
                .param("code", "code123")
                .param("state", "bad"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.containsString("error=")));
    }

    @Test
    void testSmsCodeLogin_NotAvailable() throws Exception {
        com.resume.user.auth.AuthProvider sms = mock(com.resume.user.auth.AuthProvider.class);
        when(authProviderRegistry.getLoginProvider("sms_code")).thenReturn(sms);
        when(sms.isConfigured()).thenReturn(false);

        mockMvc.perform(post("/auth/login/sms_code")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"13800138000\",\"code\":\"123456\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testMethods_ReturnsConfiguredProviders() throws Exception {
        mockMvc.perform(get("/auth/methods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
