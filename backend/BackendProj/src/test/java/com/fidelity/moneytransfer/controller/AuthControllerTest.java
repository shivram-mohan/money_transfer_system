package com.fidelity.moneytransfer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fidelity.moneytransfer.config.GlobalExceptionHandler;
import com.fidelity.moneytransfer.config.JwtUtil;
import com.fidelity.moneytransfer.dto.*;
import com.fidelity.moneytransfer.entity.AppUser;
import com.fidelity.moneytransfer.repository.UserRepository;
import com.fidelity.moneytransfer.service.OtpService;
import com.fidelity.moneytransfer.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtil jwtUtil;
    @Mock private UserRepository userRepository;
    @Mock private UserService userService;
    @Mock private OtpService otpService;
    @Mock private UserDetailsService userDetailsService;

    @InjectMocks private AuthController controller;

    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private UserDetails userDetails(String role) {
        return User.builder().username("john").password("pw")
                .authorities(List.of(new SimpleGrantedAuthority(role))).build();
    }

    private AppUser appUser() {
        return AppUser.builder().id(1L).username("john").name("John")
                .email("john@example.com").role("USER").status("ACTIVE").accountId(100L).build();
    }

    // ─── SIGNUP ───────────────────────────────────────────────────────

    @Test
    void verifyAccount_success() throws Exception {
        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("john@example.com")).thenReturn(false);

        VerifyAccountRequest req = new VerifyAccountRequest("john", "john@example.com");
        mockMvc.perform(post("/api/v1/auth/signup/verify-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.email").value("jo***@example.com"));
    }

    @Test
    void verifyAccount_shortEmailNotMasked() throws Exception {
        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("ab@x.com")).thenReturn(false);

        VerifyAccountRequest req = new VerifyAccountRequest("john", "ab@x.com");
        mockMvc.perform(post("/api/v1/auth/signup/verify-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ab@x.com"));
    }

    @Test
    void verifyAccount_duplicateUsername_422() throws Exception {
        when(userRepository.existsByUsername("john")).thenReturn(true);
        VerifyAccountRequest req = new VerifyAccountRequest("john", "john@example.com");
        mockMvc.perform(post("/api/v1/auth/signup/verify-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void verifyAccount_duplicateEmail_422() throws Exception {
        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("john@example.com")).thenReturn(true);
        VerifyAccountRequest req = new VerifyAccountRequest("john", "john@example.com");
        mockMvc.perform(post("/api/v1/auth/signup/verify-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void verifyAccount_invalidEmail_422_validation() throws Exception {
        VerifyAccountRequest req = new VerifyAccountRequest("john", "not-an-email");
        mockMvc.perform(post("/api/v1/auth/signup/verify-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void verifySignupOtp_valid() throws Exception {
        when(otpService.verifyOtp("john@example.com", "123456", "SIGNUP")).thenReturn(true);
        VerifyOtpRequest req = new VerifyOtpRequest("john@example.com", "123456", "SIGNUP");
        mockMvc.perform(post("/api/v1/auth/signup/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void verifySignupOtp_invalid_422() throws Exception {
        when(otpService.verifyOtp(anyString(), anyString(), eq("SIGNUP"))).thenReturn(false);
        VerifyOtpRequest req = new VerifyOtpRequest("john@example.com", "000000", "SIGNUP");
        mockMvc.perform(post("/api/v1/auth/signup/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void setPassword_createsUserAndReturnsTokens() throws Exception {
        UserResponseDto dto = UserResponseDto.builder().username("john").name("John").accountId(null).build();
        when(userService.completeSignup(any())).thenReturn(dto);
        when(userDetailsService.loadUserByUsername("john")).thenReturn(userDetails("ROLE_USER"));
        when(jwtUtil.generateToken(any(), eq("USER"), isNull())).thenReturn("access");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("refresh");
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(600000L);

        SetPasswordRequest req = new SetPasswordRequest("john", "john@example.com", "John", "pw");
        mockMvc.perform(post("/api/v1/auth/signup/set-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("access"))
                .andExpect(jsonPath("$.refreshToken").value("refresh"));
    }

    // ─── LOGIN ──────────────────────────────────────────────────────────

    @Test
    void loginStep1_success_sendsOtp() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken("john", "pw"));
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(appUser()));

        LoginOtpRequest req = new LoginOtpRequest("john", "pw");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void loginStep1_userNotFoundAfterAuth_401() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken("john", "pw"));
        when(userRepository.findByUsername("john")).thenReturn(Optional.empty());
        LoginOtpRequest req = new LoginOtpRequest("john", "pw");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginVerifyOtp_userNotFound_401() throws Exception {
        when(userRepository.findByUsername("john")).thenReturn(Optional.empty());
        LoginVerifyRequest req = new LoginVerifyRequest("john", "123456");
        mockMvc.perform(post("/api/v1/auth/login/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginStep1_badCredentials_401() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad"));
        LoginOtpRequest req = new LoginOtpRequest("john", "wrong");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginVerifyOtp_success_user() throws Exception {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(appUser()));
        when(otpService.verifyOtp("john@example.com", "123456", "LOGIN")).thenReturn(true);
        when(userDetailsService.loadUserByUsername("john")).thenReturn(userDetails("ROLE_USER"));
        when(jwtUtil.generateToken(any(), eq("USER"), eq(100L))).thenReturn("access");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("refresh");
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(600000L);

        LoginVerifyRequest req = new LoginVerifyRequest("john", "123456");
        mockMvc.perform(post("/api/v1/auth/login/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void loginVerifyOtp_adminRole() throws Exception {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(appUser()));
        when(otpService.verifyOtp(anyString(), anyString(), eq("LOGIN"))).thenReturn(true);
        when(userDetailsService.loadUserByUsername("john")).thenReturn(userDetails("ROLE_ADMIN"));
        when(jwtUtil.generateToken(any(), eq("ADMIN"), any())).thenReturn("access");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("refresh");
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(600000L);

        LoginVerifyRequest req = new LoginVerifyRequest("john", "123456");
        mockMvc.perform(post("/api/v1/auth/login/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void loginVerifyOtp_invalidOtp_422() throws Exception {
        // OTP fails before the user-details lookup, so no token is issued.
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(appUser()));
        when(otpService.verifyOtp(anyString(), anyString(), eq("LOGIN"))).thenReturn(false);

        LoginVerifyRequest req = new LoginVerifyRequest("john", "000000");
        mockMvc.perform(post("/api/v1/auth/login/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity());
    }

    // ─── FORGOT / RESET ─────────────────────────────────────────────────

    @Test
    void forgotPassword_success() throws Exception {
        when(userService.getUserForPasswordReset("john")).thenReturn(appUser());
        ForgotPasswordRequest req = new ForgotPasswordRequest("john");
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void forgotPassword_blankEmail_422() throws Exception {
        AppUser noEmail = appUser();
        noEmail.setEmail("");
        when(userService.getUserForPasswordReset("john")).thenReturn(noEmail);
        ForgotPasswordRequest req = new ForgotPasswordRequest("john");
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void forgotPassword_nullEmail_422() throws Exception {
        AppUser noEmail = appUser();
        noEmail.setEmail(null);
        when(userService.getUserForPasswordReset("john")).thenReturn(noEmail);
        ForgotPasswordRequest req = new ForgotPasswordRequest("john");
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void resetPassword_success() throws Exception {
        when(userService.getUserForPasswordReset("john")).thenReturn(appUser());
        when(otpService.verifyOtp("john@example.com", "123456", "RESET")).thenReturn(true);
        ResetPasswordRequest req = new ResetPasswordRequest("john", "123456", "newpw");
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void resetPassword_invalidOtp_422() throws Exception {
        when(userService.getUserForPasswordReset("john")).thenReturn(appUser());
        when(otpService.verifyOtp(anyString(), anyString(), eq("RESET"))).thenReturn(false);
        ResetPasswordRequest req = new ResetPasswordRequest("john", "000000", "newpw");
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity());
    }

    // ─── ADMIN LOGIN ────────────────────────────────────────────────────

    @Test
    void adminLogin_success() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails("ROLE_ADMIN"), null, userDetails("ROLE_ADMIN").getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtUtil.generateToken(any(), eq("ADMIN"), isNull())).thenReturn("access");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("refresh");
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(600000L);

        AuthRequest req = new AuthRequest("admin", "admin123");
        mockMvc.perform(post("/api/v1/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void adminLogin_notAdmin_401() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails("ROLE_USER"), null, userDetails("ROLE_USER").getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(auth);

        AuthRequest req = new AuthRequest("john", "pw");
        mockMvc.perform(post("/api/v1/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminLogin_badCredentials_401() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad"));
        AuthRequest req = new AuthRequest("admin", "wrong");
        mockMvc.perform(post("/api/v1/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    // ─── REFRESH ────────────────────────────────────────────────────────

    @Test
    void refresh_success() throws Exception {
        when(jwtUtil.extractUsername("rt")).thenReturn("john");
        when(userDetailsService.loadUserByUsername("john")).thenReturn(userDetails("ROLE_USER"));
        when(jwtUtil.validateRefreshToken(eq("rt"), any())).thenReturn(true);
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(appUser()));
        when(jwtUtil.generateToken(any(), eq("USER"), eq(100L))).thenReturn("newAccess");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("newRefresh");
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(600000L);

        RefreshTokenRequest req = new RefreshTokenRequest("rt");
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("newAccess"));
    }

    @Test
    void refresh_adminRole() throws Exception {
        AppUser admin = AppUser.builder().id(2L).username("john").name("Admin")
                .email("a@x.com").role("ADMIN").status("ACTIVE").accountId(null).build();
        when(jwtUtil.extractUsername("rt")).thenReturn("john");
        when(userDetailsService.loadUserByUsername("john")).thenReturn(userDetails("ROLE_ADMIN"));
        when(jwtUtil.validateRefreshToken(eq("rt"), any())).thenReturn(true);
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(admin));
        when(jwtUtil.generateToken(any(), eq("ADMIN"), isNull())).thenReturn("newAccess");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("newRefresh");
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(600000L);

        RefreshTokenRequest req = new RefreshTokenRequest("rt");
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void refresh_userNotFound_401() throws Exception {
        when(jwtUtil.extractUsername("rt")).thenReturn("john");
        when(userDetailsService.loadUserByUsername("john")).thenReturn(userDetails("ROLE_USER"));
        when(jwtUtil.validateRefreshToken(eq("rt"), any())).thenReturn(true);
        when(userRepository.findByUsername("john")).thenReturn(Optional.empty());

        RefreshTokenRequest req = new RefreshTokenRequest("rt");
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_extractFails_401() throws Exception {
        when(jwtUtil.extractUsername("bad")).thenThrow(new RuntimeException("malformed"));
        RefreshTokenRequest req = new RefreshTokenRequest("bad");
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_validationFails_401() throws Exception {
        when(jwtUtil.extractUsername("rt")).thenReturn("john");
        when(userDetailsService.loadUserByUsername("john")).thenReturn(userDetails("ROLE_USER"));
        when(jwtUtil.validateRefreshToken(eq("rt"), any())).thenReturn(false);

        RefreshTokenRequest req = new RefreshTokenRequest("rt");
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    // ─── VERIFY PASSWORD ────────────────────────────────────────────────

    @Test
    void verifyPassword_success() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken("john", "pw"));
        VerifyPasswordRequest req = new VerifyPasswordRequest("john", "pw");
        mockMvc.perform(post("/api/v1/auth/verify-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void verifyPassword_wrong_422() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad"));
        VerifyPasswordRequest req = new VerifyPasswordRequest("john", "wrong");
        mockMvc.perform(post("/api/v1/auth/verify-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity());
    }
}
