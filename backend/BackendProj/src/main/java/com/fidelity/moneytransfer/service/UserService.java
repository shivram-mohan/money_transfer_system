package com.fidelity.moneytransfer.service;

import com.fidelity.moneytransfer.dto.CreateUserRequest;
import com.fidelity.moneytransfer.dto.DeactivateUserRequest;
import com.fidelity.moneytransfer.dto.SignupRequest;
import com.fidelity.moneytransfer.dto.UserResponseDto;
import com.fidelity.moneytransfer.entity.Account;
import com.fidelity.moneytransfer.entity.AppUser;
import com.fidelity.moneytransfer.enums.AccountStatus;
import com.fidelity.moneytransfer.exception.AccountNotFoundException;
import com.fidelity.moneytransfer.repository.AccountRepository;
import com.fidelity.moneytransfer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final PasswordEncoder passwordEncoder;

    // ─── EXISTING METHODS ─────────────────────────────────────────────

    public List<UserResponseDto> getAllUsers() {
        log.debug("Fetching all users");
        return userRepository.findAll()
                .stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    public List<UserResponseDto> getPendingUsers() {
        log.debug("Fetching pending users");
        return userRepository.findByStatus("PENDING")
                .stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    public UserResponseDto getUserById(Long userId) {
        log.debug("Fetching user with id: {}", userId);
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new AccountNotFoundException(
                        "User not found with id: " + userId));
        return mapToUserResponse(user);
    }

    public UserResponseDto getUserByUsername(String username) {
        log.debug("Fetching user with username: {}", username);
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AccountNotFoundException(
                        "User not found with username: " + username));
        return mapToUserResponse(user);
    }

    @Transactional
    public UserResponseDto createUser(CreateUserRequest request, String createdBy) {
        log.info("Creating new user: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException(
                    "Username already exists: " + request.getUsername());
        }

        Account account = Account.builder()
                .id(accountService.generateUniqueAccountId())
                .holderName(request.getName())
                .balance(request.getInitialBalance())
                .status(AccountStatus.ACTIVE)
                .version(0)
                .build();
        Account savedAccount = accountRepository.save(account);

        AppUser user = AppUser.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .email(request.getEmail())
                .role("USER")
                .status("ACTIVE")
                .accountId(savedAccount.getId())
                .createdBy(createdBy)
                .approvedBy(createdBy)
                .approvedDate(LocalDateTime.now())
                .build();

        AppUser savedUser = userRepository.save(user);
        log.info("User created with id: {}", savedUser.getId());

        return mapToUserResponse(savedUser);
    }

    @Transactional
    public UserResponseDto activateUser(Long userId) {
        log.info("Activating user id: {}", userId);

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new AccountNotFoundException(
                        "User not found with id: " + userId));

        user.setStatus("ACTIVE");

        if (user.getAccountId() != null) {
            accountRepository.findById(user.getAccountId())
                    .ifPresent(account -> {
                        account.setStatus(AccountStatus.ACTIVE);
                        accountRepository.save(account);
                    });
        }

        return mapToUserResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponseDto deactivateUser(DeactivateUserRequest request) {
        log.info("Deactivating user id: {}", request.getUserId());

        AppUser user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new AccountNotFoundException(
                        "User not found with id: " + request.getUserId()));

        user.setStatus("INACTIVE");

        if (user.getAccountId() != null) {
            accountRepository.findById(user.getAccountId())
                    .ifPresent(account -> {
                        account.setStatus(AccountStatus.LOCKED);
                        accountRepository.save(account);
                    });
        }

        return mapToUserResponse(userRepository.save(user));
    }

    // ─── NEW METHODS FOR SIGNUP FLOW ──────────────────────────────────

    @Transactional
    public UserResponseDto signupUser(SignupRequest request) {
        log.info("User signup request: {}", request.getUsername());

        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException(
                    "Username already exists: " + request.getUsername());
        }

        // Validate email uniqueness (optional)
        if (request.getEmail() != null &&
                !request.getEmail().isEmpty()) {
            userRepository.findAll().stream()
                    .filter(u -> request.getEmail().equals(u.getEmail()))
                    .findFirst()
                    .ifPresent(u -> {
                        throw new IllegalArgumentException(
                                "Email already registered");
                    });
        }

        // Create account with LOCKED status
        Account account = Account.builder()
                .id(accountService.generateUniqueAccountId())
                .holderName(request.getName())
                .balance(request.getInitialBalance())
                .status(AccountStatus.LOCKED) // Locked until approved
                .version(0)
                .build();
        Account savedAccount = accountRepository.save(account);

        // Create user with PENDING status
        AppUser user = AppUser.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .email(request.getEmail())
                .role("USER")
                .status("PENDING") // Needs admin approval
                .accountId(savedAccount.getId())
                .createdBy("self-registration")
                .build();

        AppUser savedUser = userRepository.save(user);
        log.info("User signup successful - pending approval: {}",
                savedUser.getId());

        return mapToUserResponse(savedUser);
    }

    @Transactional
    public UserResponseDto approveUser(Long userId, String approvedBy) {
        log.info("Approving user id: {} by: {}", userId, approvedBy);

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new AccountNotFoundException(
                        "User not found with id: " + userId));

        if (!user.getStatus().equals("PENDING")) {
            throw new IllegalArgumentException(
                    "User is not in pending status. Current status: " +
                            user.getStatus());
        }

        // Activate user
        user.setStatus("ACTIVE");
        user.setApprovedBy(approvedBy);
        user.setApprovedDate(LocalDateTime.now());

        // Activate their account
        if (user.getAccountId() != null) {
            accountRepository.findById(user.getAccountId())
                    .ifPresent(account -> {
                        account.setStatus(AccountStatus.ACTIVE);
                        accountRepository.save(account);
                        log.info("Account {} activated", account.getId());
                    });
        }

        AppUser approvedUser = userRepository.save(user);
        log.info("User {} approved successfully", userId);

        return mapToUserResponse(approvedUser);
    }

    @Transactional
    public void rejectUser(Long userId, String reason) {
        log.info("Rejecting user id: {} - Reason: {}", userId, reason);

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new AccountNotFoundException(
                        "User not found with id: " + userId));

        if (!user.getStatus().equals("PENDING")) {
            throw new IllegalArgumentException(
                    "User is not in pending status. Current status: " +
                            user.getStatus());
        }

        // Delete the user's account if exists
        if (user.getAccountId() != null) {
            accountRepository.deleteById(user.getAccountId());
            log.info("Deleted account {} for rejected user",
                    user.getAccountId());
        }

        // Delete the user
        userRepository.deleteById(userId);
        log.info("User {} rejected and deleted. Reason: {}", userId, reason);
    }

    // ─── HELPER METHODS ───────────────────────────────────────────────

    private UserResponseDto mapToUserResponse(AppUser user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .role(user.getRole())
                .status(user.getStatus())
                .accountId(user.getAccountId())
                .email(user.getEmail())
                .createdDate(user.getCreatedDate())
                .approvedBy(user.getApprovedBy())
                .approvedDate(user.getApprovedDate())
                .build();
    }
}