package com.fidelity.moneytransfer.controller;

import com.fidelity.moneytransfer.config.JwtUtil;
import com.fidelity.moneytransfer.dto.*;
import com.fidelity.moneytransfer.repository.UserRepository;
import com.fidelity.moneytransfer.service.OtpService;
import com.fidelity.moneytransfer.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final UserService userService;
    private final OtpService otpService;

    // ─── SIGNUP FLOW (3 steps) ────────────────────────────────────────

    /**
     * Step 1: User provides username + email. We verify the email belongs to
     * them by sending an OTP. Bank account linking happens later, post-login.
     */
    @PostMapping("/signup/verify-account")
    public ResponseEntity<OtpResponse> verifyAccount(
            @Valid @RequestBody VerifyAccountRequest request) {

        log.info("Signup step 1 - sending OTP for username: {}", request.getUsername());

        // Check if username already taken
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }

        // Check if email already in use by another registered user
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new IllegalArgumentException(
                    "An account with this email already exists");
        }

        // Send OTP to verify email ownership
        otpService.generateAndSendOtp(request.getEmail(), "SIGNUP");

        return ResponseEntity.ok(OtpResponse.builder()
                .message("OTP sent to your email")
                .email(maskEmail(request.getEmail()))
                .success(true)
                .build());
    }

    /**
     * Step 2: User provides the OTP received via email.
     */
    @PostMapping("/signup/verify-otp")
    public ResponseEntity<OtpResponse> verifySignupOtp(
            @Valid @RequestBody VerifyOtpRequest request) {

        log.info("Signup step 2 - verifying OTP for: {}", request.getEmail());

        boolean isValid = otpService.verifyOtp(
                request.getEmail(), request.getOtp(), "SIGNUP");

        if (!isValid) {
            throw new IllegalArgumentException("Invalid or expired OTP");
        }

        return ResponseEntity.ok(OtpResponse.builder()
                .message("OTP verified successfully. Please set your password.")
                .email(request.getEmail())
                .success(true)
                .build());
    }

    /**
     * Step 3: User sets password. Account is created and activated immediately.
     */
    @PostMapping("/signup/set-password")
    public ResponseEntity<UserResponseDto> setPassword(
            @Valid @RequestBody SetPasswordRequest request) {

        log.info("Signup step 3 - setting password for: {}", request.getUsername());

        UserResponseDto user = userService.completeSignup(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    // ─── USER LOGIN FLOW (2 steps) ───────────────────────────────────

    /**
     * Step 1: User provides username + password. If valid, OTP is sent to email.
     */
    @PostMapping("/login")
    public ResponseEntity<OtpResponse> loginStep1(
            @Valid @RequestBody LoginOtpRequest request) {

        log.info("Login step 1 - credentials check for: {}", request.getUsername());

        try {
            // Validate credentials
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(), request.getPassword()));

            // Get user email
            var appUser = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new BadCredentialsException("User not found"));

            // Send OTP to user's email
            otpService.generateAndSendOtp(appUser.getEmail(), "LOGIN");

            return ResponseEntity.ok(OtpResponse.builder()
                    .message("OTP sent to your registered email")
                    .email(maskEmail(appUser.getEmail()))
                    .success(true)
                    .build());

        } catch (BadCredentialsException e) {
            log.error("Bad credentials for: {}", request.getUsername());
            throw new BadCredentialsException("Invalid username or password");
        }
    }

    /**
     * Step 2: User provides username + password + OTP. Returns JWT on success.
     */
    @PostMapping("/login/verify-otp")
    public ResponseEntity<AuthResponse> loginVerifyOtp(
            @Valid @RequestBody LoginVerifyRequest request) {

        log.info("Login step 2 - OTP verification for: {}", request.getUsername());

        // Re-authenticate credentials
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(), request.getPassword()));

        // Get user
        var appUser = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        // Verify OTP
        boolean isValid = otpService.verifyOtp(
                appUser.getEmail(), request.getOtp(), "LOGIN");

        if (!isValid) {
            throw new IllegalArgumentException("Invalid or expired OTP");
        }

        // Generate JWT
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String role = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))
                ? "ADMIN" : "USER";

        Long accountId = appUser.getAccountId();
        String holderName = appUser.getName();

        String token = jwtUtil.generateToken(userDetails, role, accountId);

        return ResponseEntity.ok(AuthResponse.builder()
                .token(token)
                .username(request.getUsername())
                .role(role)
                .accountId(accountId)
                .holderName(holderName)
                .expiresIn(86400000L)
                .build());
    }

    // ─── FORGOT PASSWORD FLOW (2 steps) ──────────────────────────────

    /**
     * Step 1: User provides their username. If it exists, a reset OTP is sent
     * to the registered email. The response masks the email and never reveals
     * whether the account exists in a way that aids enumeration.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<OtpResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        log.info("Forgot password - sending reset OTP for username: {}", request.getUsername());

        var appUser = userService.getUserForPasswordReset(request.getUsername());

        if (appUser.getEmail() == null || appUser.getEmail().isBlank()) {
            throw new IllegalArgumentException(
                    "No email is registered for this account. Please contact support.");
        }

        otpService.generateAndSendOtp(appUser.getEmail(), "RESET");

        return ResponseEntity.ok(OtpResponse.builder()
                .message("A password reset OTP has been sent to your registered email")
                .email(maskEmail(appUser.getEmail()))
                .success(true)
                .build());
    }

    /**
     * Step 2: User provides username + OTP + new password. The OTP is verified
     * and, on success, the password is updated.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<OtpResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        log.info("Reset password - verifying OTP for username: {}", request.getUsername());

        var appUser = userService.getUserForPasswordReset(request.getUsername());

        boolean isValid = otpService.verifyOtp(
                appUser.getEmail(), request.getOtp(), "RESET");

        if (!isValid) {
            throw new IllegalArgumentException("Invalid or expired OTP");
        }

        userService.resetPassword(request.getUsername(), request.getPassword());

        return ResponseEntity.ok(OtpResponse.builder()
                .message("Password reset successfully. You can now log in with your new password.")
                .email(maskEmail(appUser.getEmail()))
                .success(true)
                .build());
    }

    // ─── ADMIN LOGIN (direct, no OTP) ────────────────────────────────

    @PostMapping("/admin/login")
    public ResponseEntity<AuthResponse> adminLogin(
            @Valid @RequestBody AuthRequest request) {

        log.info("Admin login attempt for: {}", request.getUsername());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(), request.getPassword()));

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            // Verify this is actually an admin
            boolean isAdmin = userDetails.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            if (!isAdmin) {
                throw new BadCredentialsException("Access denied. Not an admin account.");
            }

            String token = jwtUtil.generateToken(userDetails, "ADMIN", null);

            return ResponseEntity.ok(AuthResponse.builder()
                    .token(token)
                    .username(request.getUsername())
                    .role("ADMIN")
                    .accountId(null)
                    .holderName(request.getUsername())
                    .expiresIn(86400000L)
                    .build());

        } catch (BadCredentialsException e) {
            log.error("Admin login failed for: {}", request.getUsername());
            throw new BadCredentialsException("Invalid admin credentials");
        }
    }

    // ─── HELPER ──────────────────────────────────────────────────────

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) return email;
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }
}
