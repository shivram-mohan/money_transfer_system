package com.fidelity.moneytransfer.controller;

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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthControllerTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtil jwtUtil;
    @Mock private UserRepository userRepository;
    @Mock private UserService userService;
    @Mock private OtpService otpService;
    @Mock private UserDetailsService userDetailsService;
    @InjectMocks private AuthController controller;

    private AppUser appUser;

    @BeforeEach
    void setUp() {
        appUser = AppUser.builder()
                .id(1L).username("john").password("hash").name("John Doe")
                .email("john.doe@example.com").role("USER").status("ACTIVE")
                .accountId(1001L).build();

        when(jwtUtil.generateToken(any(), any(), any())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("refresh-token");
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(600000L);
    }

    private UserDetails userDetails(String role) {
        return User.builder().username("john").password("hash")
                .authorities(List.of(new SimpleGrantedAuthority(role))).build();
    }

    private Authentication authWith(String role) {
        // Build a real token (no nested stubbing) whose principal is the
        // UserDetails and whose authorities drive role derivation.
        UserDetails details = userDetails(role);
        return new UsernamePasswordAuthenticationToken(
                details, null, details.getAuthorities());
    }

    // ─── SIGNUP ──────────────────────────────────────────────────────

    @Test
    void verifyAccount_Success_MasksEmail() {
        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("john.doe@example.com")).thenReturn(false);

        ResponseEntity<OtpResponse> r = controller.verifyAccount(
                new VerifyAccountRequest("john", "john.doe@example.com"));

        assertTrue(r.getBody().isSuccess());
        assertEquals("jo***@example.com", r.getBody().getEmail());
        verify(otpService).generateAndSendOtp("john.doe@example.com", "SIGNUP");
    }

    @Test
    void verifyAccount_UsernameTaken_Throws() {
        when(userRepository.existsByUsername("john")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> controller.verifyAccount(
                new VerifyAccountRequest("john", "john.doe@example.com")));
    }

    @Test
    void verifyAccount_EmailTaken_Throws() {
        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("john.doe@example.com")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> controller.verifyAccount(
                new VerifyAccountRequest("john", "john.doe@example.com")));
    }

    @Test
    void verifySignupOtp_Valid() {
        when(otpService.verifyOtp("e@x.com", "123456", "SIGNUP")).thenReturn(true);
        ResponseEntity<OtpResponse> r = controller.verifySignupOtp(
                new VerifyOtpRequest("e@x.com", "123456", "SIGNUP"));
        assertTrue(r.getBody().isSuccess());
    }

    @Test
    void verifySignupOtp_Invalid_Throws() {
        when(otpService.verifyOtp(any(), any(), any())).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> controller.verifySignupOtp(
                new VerifyOtpRequest("e@x.com", "000000", "SIGNUP")));
    }

    @Test
    void setPassword_CreatesUser_AutoLogin201() {
        UserResponseDto created = UserResponseDto.builder()
                .username("john").name("John Doe").accountId(null).build();
        when(userService.completeSignup(any())).thenReturn(created);
        when(userDetailsService.loadUserByUsername("john")).thenReturn(userDetails("ROLE_USER"));

        ResponseEntity<AuthResponse> r = controller.setPassword(
                new SetPasswordRequest("john", "john.doe@example.com", "John Doe", "pw"));

        assertEquals(HttpStatus.CREATED, r.getStatusCode());
        assertEquals("access-token", r.getBody().getToken());
        assertEquals("USER", r.getBody().getRole());
    }

    // ─── LOGIN ───────────────────────────────────────────────────────

    @Test
    void loginStep1_Success_SendsOtp() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(appUser));

        ResponseEntity<OtpResponse> r = controller.loginStep1(
                new LoginOtpRequest("john", "pw"));

        assertTrue(r.getBody().isSuccess());
        verify(otpService).generateAndSendOtp("john.doe@example.com", "LOGIN");
    }

    @Test
    void loginStep1_BadCredentials_Throws() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad"));
        assertThrows(BadCredentialsException.class,
                () -> controller.loginStep1(new LoginOtpRequest("john", "wrong")));
    }

    @Test
    void loginVerifyOtp_Success_ReturnsTokens() {
        when(authenticationManager.authenticate(any())).thenReturn(authWith("ROLE_USER"));
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(appUser));
        when(otpService.verifyOtp("john.doe@example.com", "123456", "LOGIN")).thenReturn(true);

        ResponseEntity<AuthResponse> r = controller.loginVerifyOtp(
                new LoginVerifyRequest("john", "pw", "123456"));

        assertEquals("access-token", r.getBody().getToken());
        assertEquals("USER", r.getBody().getRole());
        assertEquals(1001L, r.getBody().getAccountId());
    }

    @Test
    void loginVerifyOtp_InvalidOtp_Throws() {
        when(authenticationManager.authenticate(any())).thenReturn(authWith("ROLE_USER"));
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(appUser));
        when(otpService.verifyOtp(any(), any(), any())).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> controller.loginVerifyOtp(
                new LoginVerifyRequest("john", "pw", "000000")));
    }

    // ─── FORGOT / RESET ──────────────────────────────────────────────

    @Test
    void forgotPassword_Success() {
        when(userService.getUserForPasswordReset("john")).thenReturn(appUser);
        ResponseEntity<OtpResponse> r = controller.forgotPassword(
                new ForgotPasswordRequest("john"));
        assertTrue(r.getBody().isSuccess());
        verify(otpService).generateAndSendOtp("john.doe@example.com", "RESET");
    }

    @Test
    void forgotPassword_NoEmail_Throws() {
        appUser.setEmail("  ");
        when(userService.getUserForPasswordReset("john")).thenReturn(appUser);
        assertThrows(IllegalArgumentException.class,
                () -> controller.forgotPassword(new ForgotPasswordRequest("john")));
    }

    @Test
    void resetPassword_Success() {
        when(userService.getUserForPasswordReset("john")).thenReturn(appUser);
        when(otpService.verifyOtp("john.doe@example.com", "123456", "RESET")).thenReturn(true);

        ResponseEntity<OtpResponse> r = controller.resetPassword(
                new ResetPasswordRequest("john", "123456", "newpw"));

        assertTrue(r.getBody().isSuccess());
        verify(userService).resetPassword("john", "newpw");
    }

    @Test
    void resetPassword_InvalidOtp_Throws() {
        when(userService.getUserForPasswordReset("john")).thenReturn(appUser);
        when(otpService.verifyOtp(any(), any(), any())).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> controller.resetPassword(
                new ResetPasswordRequest("john", "000000", "newpw")));
    }

    // ─── ADMIN LOGIN ─────────────────────────────────────────────────

    @Test
    void adminLogin_Success() {
        when(authenticationManager.authenticate(any())).thenReturn(authWith("ROLE_ADMIN"));

        ResponseEntity<AuthResponse> r = controller.adminLogin(
                new AuthRequest("admin", "admin123"));

        assertEquals("ADMIN", r.getBody().getRole());
        assertNull(r.getBody().getAccountId());
    }

    @Test
    void adminLogin_NotAdmin_Throws() {
        when(authenticationManager.authenticate(any())).thenReturn(authWith("ROLE_USER"));
        assertThrows(BadCredentialsException.class,
                () -> controller.adminLogin(new AuthRequest("john", "pw")));
    }

    @Test
    void adminLogin_BadCredentials_Throws() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad"));
        assertThrows(BadCredentialsException.class,
                () -> controller.adminLogin(new AuthRequest("admin", "wrong")));
    }

    // ─── REFRESH ─────────────────────────────────────────────────────

    @Test
    void refreshToken_Success_RotatesTokens() {
        when(jwtUtil.extractUsername("refresh-in")).thenReturn("john");
        when(userDetailsService.loadUserByUsername("john")).thenReturn(userDetails("ROLE_USER"));
        when(jwtUtil.validateRefreshToken(eq("refresh-in"), any())).thenReturn(true);
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(appUser));

        ResponseEntity<AuthResponse> r = controller.refreshToken(
                new RefreshTokenRequest("refresh-in"));

        assertEquals("access-token", r.getBody().getToken());
        assertEquals("refresh-token", r.getBody().getRefreshToken());
    }

    @Test
    void refreshToken_Unparseable_Throws() {
        when(jwtUtil.extractUsername("garbage")).thenThrow(new RuntimeException("bad"));
        assertThrows(BadCredentialsException.class,
                () -> controller.refreshToken(new RefreshTokenRequest("garbage")));
    }

    @Test
    void refreshToken_InvalidRefresh_Throws() {
        when(jwtUtil.extractUsername("refresh-in")).thenReturn("john");
        when(userDetailsService.loadUserByUsername("john")).thenReturn(userDetails("ROLE_USER"));
        when(jwtUtil.validateRefreshToken(any(), any())).thenReturn(false);

        assertThrows(BadCredentialsException.class,
                () -> controller.refreshToken(new RefreshTokenRequest("refresh-in")));
    }

    // ─── VERIFY PASSWORD ─────────────────────────────────────────────

    @Test
    void verifyPassword_Correct() {
        ResponseEntity<OtpResponse> r = controller.verifyPassword(
                new VerifyPasswordRequest("john", "pw"));
        assertTrue(r.getBody().isSuccess());
    }

    @Test
    void verifyPassword_Wrong_ThrowsIllegalArgument() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad"));
        assertThrows(IllegalArgumentException.class, () -> controller.verifyPassword(
                new VerifyPasswordRequest("john", "wrong")));
    }

    // ─── EDGE CASES (role derivation, missing user, short email) ─────

    @Test
    void loginStep1_UserNotFoundAfterAuth_Throws() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.empty());
        assertThrows(BadCredentialsException.class,
                () -> controller.loginStep1(new LoginOtpRequest("john", "pw")));
    }

    @Test
    void loginVerifyOtp_UserNotFound_Throws() {
        when(authenticationManager.authenticate(any())).thenReturn(authWith("ROLE_USER"));
        when(userRepository.findByUsername("john")).thenReturn(Optional.empty());
        assertThrows(BadCredentialsException.class, () -> controller.loginVerifyOtp(
                new LoginVerifyRequest("john", "pw", "123456")));
    }

    @Test
    void loginVerifyOtp_AdminRole_DerivesAdmin() {
        when(authenticationManager.authenticate(any())).thenReturn(authWith("ROLE_ADMIN"));
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(appUser));
        when(otpService.verifyOtp(any(), any(), eq("LOGIN"))).thenReturn(true);

        ResponseEntity<AuthResponse> r = controller.loginVerifyOtp(
                new LoginVerifyRequest("john", "pw", "123456"));

        assertEquals("ADMIN", r.getBody().getRole());
    }

    @Test
    void refreshToken_UserNotFound_Throws() {
        when(jwtUtil.extractUsername("refresh-in")).thenReturn("john");
        when(userDetailsService.loadUserByUsername("john")).thenReturn(userDetails("ROLE_USER"));
        when(jwtUtil.validateRefreshToken(any(), any())).thenReturn(true);
        when(userRepository.findByUsername("john")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class,
                () -> controller.refreshToken(new RefreshTokenRequest("refresh-in")));
    }

    @Test
    void refreshToken_AdminRole_DerivesAdmin() {
        when(jwtUtil.extractUsername("refresh-in")).thenReturn("admin");
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(userDetails("ROLE_ADMIN"));
        when(jwtUtil.validateRefreshToken(any(), any())).thenReturn(true);
        AppUser admin = AppUser.builder().id(2L).username("admin").password("h")
                .name("Admin").email("a@x.com").role("ADMIN").status("ACTIVE")
                .accountId(null).build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        ResponseEntity<AuthResponse> r = controller.refreshToken(
                new RefreshTokenRequest("refresh-in"));

        assertEquals("ADMIN", r.getBody().getRole());
    }

    @Test
    void verifyAccount_ShortEmail_ReturnedUnmasked() {
        when(userRepository.existsByUsername("amy")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("a@x.com")).thenReturn(false);

        ResponseEntity<OtpResponse> r = controller.verifyAccount(
                new VerifyAccountRequest("amy", "a@x.com"));

        // '@' at index 1 (<= 2) -> maskEmail returns the address as-is
        assertEquals("a@x.com", r.getBody().getEmail());
    }
}
