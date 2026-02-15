package com.fidelity.moneytransfer.controller;

import com.fidelity.moneytransfer.config.JwtUtil;
import com.fidelity.moneytransfer.dto.AuthRequest;
import com.fidelity.moneytransfer.dto.AuthResponse;
import com.fidelity.moneytransfer.dto.SignupRequest;
import com.fidelity.moneytransfer.dto.UserResponseDto;
import com.fidelity.moneytransfer.exception.AccountNotFoundException;
import com.fidelity.moneytransfer.repository.UserRepository;
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

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody AuthRequest request) {

        log.info("Login attempt for: {}", request.getUsername());

        try {
            // Step 1: Authenticate
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
            log.info("Authentication successful for: {}",
                    request.getUsername());

            UserDetails userDetails =
                    (UserDetails) authentication.getPrincipal();

            // Step 2: Determine role
            String role = userDetails.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))
                    ? "ADMIN" : "USER";
            log.info("Role determined: {}", role);

            // Step 3: Get account ID and holder name
            Long accountId = null;
            String holderName = request.getUsername();

            if (!role.equals("ADMIN")) {
                var userOptional = userRepository
                        .findByUsername(request.getUsername());

                if (userOptional.isPresent()) {
                    accountId = userOptional.get().getAccountId();
                    holderName = userOptional.get().getName();
                    log.info("Found user account: {}", accountId);
                } else {
                    log.warn("No user record found for: {}", request.getUsername());
                            throw new AccountNotFoundException(holderName);
                }
            }

            // Step 4: Generate JWT
            String token = jwtUtil.generateToken(
                    userDetails, role, accountId
            );
            log.info("JWT generated successfully");

            return ResponseEntity.ok(AuthResponse.builder()
                    .token(token)
                    .username(request.getUsername())
                    .role(role)
                    .accountId(accountId)
                    .holderName(holderName)
                    .expiresIn(900000L)
                    .build());

        } catch (BadCredentialsException e) {
            log.error("Bad credentials for: {}", request.getUsername());
            throw new BadCredentialsException(
                    "Invalid username or password"
            );
        } catch (Exception e) {
            log.error("Login error for {}: {}",
                    request.getUsername(), e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<UserResponseDto> signup(
            @Valid @RequestBody SignupRequest request) {

        log.info("Signup request for username: {}", request.getUsername());

        try {
            UserResponseDto user = userService.signupUser(request);
            log.info("Signup successful - awaiting approval: {}",
                    request.getUsername());

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(user);

        } catch (IllegalArgumentException e) {
            log.error("Signup failed: {}", e.getMessage());
            throw e;
        }
    }
}