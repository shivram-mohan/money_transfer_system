package com.fidelity.moneytransfer.controller;

import com.fidelity.moneytransfer.dto.CreateUserRequest;
import com.fidelity.moneytransfer.dto.DeactivateUserRequest;
import com.fidelity.moneytransfer.dto.LinkBankRequest;
import com.fidelity.moneytransfer.dto.LinkBankResponse;
import com.fidelity.moneytransfer.dto.UserResponseDto;
import com.fidelity.moneytransfer.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        log.info("Admin: Fetching all users");
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable Long id) {
        log.info("Fetching user with id: {}", id);
        return ResponseEntity.ok(userService.getUserById(id));
    }

    /**
     * Link a verified bank account to the currently logged-in user.
     * Authenticated (any user), not admin-only.
     */
    @PostMapping("/link-bank")
    public ResponseEntity<LinkBankResponse> linkBankAccount(
            @Valid @RequestBody LinkBankRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        log.info("User {} linking bank account {}", username, request.getAccountNumber());
        return ResponseEntity.ok(
                userService.linkBankAccount(username, request.getAccountNumber()));
    }

    @PostMapping
    public ResponseEntity<UserResponseDto> createUser(
            @Valid @RequestBody CreateUserRequest request,
            Authentication authentication) {
        log.info("Admin: Creating user: {}", request.getUsername());
        String createdBy = authentication.getName();
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userService.createUser(request, createdBy));
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<UserResponseDto> activateUser(@PathVariable Long id) {
        log.info("Admin: Activating user id: {}", id);
        return ResponseEntity.ok(userService.activateUser(id));
    }

    @PutMapping("/deactivate")
    public ResponseEntity<UserResponseDto> deactivateUser(
            @RequestBody DeactivateUserRequest request) {
        log.info("Admin: Deactivating user id: {}", request.getUserId());
        return ResponseEntity.ok(userService.deactivateUser(request));
    }
}
