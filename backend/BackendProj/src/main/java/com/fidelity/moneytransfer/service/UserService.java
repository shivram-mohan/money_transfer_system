package com.fidelity.moneytransfer.service;

import com.fidelity.moneytransfer.dto.CreateUserRequest;
import com.fidelity.moneytransfer.dto.DeactivateUserRequest;
import com.fidelity.moneytransfer.dto.SetPasswordRequest;
import com.fidelity.moneytransfer.dto.UserResponseDto;
import com.fidelity.moneytransfer.entity.Account;
import com.fidelity.moneytransfer.entity.AppUser;
import com.fidelity.moneytransfer.entity.BankDetails;
import com.fidelity.moneytransfer.enums.AccountStatus;
import com.fidelity.moneytransfer.exception.AccountNotFoundException;
import com.fidelity.moneytransfer.repository.AccountRepository;
import com.fidelity.moneytransfer.repository.BankDetailsRepository;
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
    private final BankDetailsRepository bankDetailsRepository;

    // ─── QUERY METHODS ───────────────────────────────────────────────

    public List<UserResponseDto> getAllUsers() {
        log.debug("Fetching all users");
        return userRepository.findAll()
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

    // ─── SIGNUP (OTP-verified, no admin approval needed) ─────────────

    @Transactional
    public UserResponseDto completeSignup(SetPasswordRequest request) {
        log.info("Completing signup for username: {}", request.getUsername());

        // Check username not taken
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException(
                    "Username already exists: " + request.getUsername());
        }

        // Get bank details
        BankDetails bankDetails = bankDetailsRepository
                .findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        if (bankDetails.getRegistered()) {
            throw new IllegalArgumentException("This account has already been registered");
        }

        // Create account with ACTIVE status (verified via OTP, no approval needed)
        Account account = Account.builder()
                .id(request.getAccountNumber())
                .holderName(bankDetails.getUserName())
                .balance(bankDetails.getBalance())
                .status(AccountStatus.ACTIVE)
                .version(0)
                .build();
        Account savedAccount = accountRepository.save(account);

        // Create user with ACTIVE status directly
        AppUser user = AppUser.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(bankDetails.getUserName())
                .email(bankDetails.getEmail())
                .role("USER")
                .status("ACTIVE")
                .accountId(savedAccount.getId())
                .createdBy("otp-verified")
                .approvedBy("otp-verified")
                .approvedDate(LocalDateTime.now())
                .build();

        AppUser savedUser = userRepository.save(user);

        // Mark bank details as registered
        bankDetails.setRegistered(true);
        bankDetailsRepository.save(bankDetails);

        log.info("User signup completed - active immediately: {}", savedUser.getId());
        return mapToUserResponse(savedUser);
    }

    // ─── ADMIN: CREATE USER DIRECTLY ─────────────────────────────────

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

    // ─── ACTIVATE / DEACTIVATE ───────────────────────────────────────

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

    // ─── HELPER ──────────────────────────────────────────────────────

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
