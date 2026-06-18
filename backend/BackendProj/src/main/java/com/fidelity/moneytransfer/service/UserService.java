package com.fidelity.moneytransfer.service;

import com.fidelity.moneytransfer.dto.CreateUserRequest;
import com.fidelity.moneytransfer.dto.DeactivateUserRequest;
import com.fidelity.moneytransfer.dto.LinkBankResponse;
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

        // Check email not already used
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new IllegalArgumentException(
                    "An account with this email already exists");
        }

        // Create user with ACTIVE status but NO bank account linked yet.
        // The user links their bank account as a separate step after login.
        AppUser user = AppUser.builder()
                .username(request.getUsername())
                // request.getPassword() is the SHA-256 hash sent by the client
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .email(request.getEmail())
                .role("USER")
                .status("ACTIVE")
                .accountId(null)
                .createdBy("otp-verified")
                .approvedBy("otp-verified")
                .approvedDate(LocalDateTime.now())
                .build();

        AppUser savedUser = userRepository.save(user);

        log.info("User signup completed (bank not yet linked): {}", savedUser.getId());
        return mapToUserResponse(savedUser);
    }

    // ─── FORGOT / RESET PASSWORD (OTP-verified) ──────────────────────

    /**
     * Looks up a user by username and returns their registered email so the
     * caller can dispatch a password-reset OTP. Throws if the user is unknown.
     */
    public AppUser getUserForPasswordReset(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AccountNotFoundException(
                        "No account found with username: " + username));
    }

    /**
     * Sets a new password for the user. Caller must have already verified the
     * reset OTP. The incoming password is the client-side SHA-256 hash, which
     * we bcrypt-encode before persisting (same scheme as signup).
     */
    @Transactional
    public void resetPassword(String username, String hashedPassword) {
        log.info("Resetting password for username: {}", username);

        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AccountNotFoundException(
                        "No account found with username: " + username));

        user.setPassword(passwordEncoder.encode(hashedPassword));
        userRepository.save(user);

        log.info("Password reset completed for username: {}", username);
    }

    // ─── LINK BANK ACCOUNT (post-signup) ─────────────────────────────

    @Transactional
    public LinkBankResponse linkBankAccount(String username, Long accountNumber) {
        log.info("Linking bank account {} for user {}", accountNumber, username);

        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AccountNotFoundException(
                        "User not found: " + username));

        if (user.getAccountId() != null) {
            throw new IllegalArgumentException(
                    "A bank account is already linked to this profile");
        }

        BankDetails bankDetails = bankDetailsRepository
                .findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Account number not found in our banking records"));

        // The user proved ownership of their email via OTP at signup, so the
        // bank record's email must match to prove the account is theirs.
        if (!bankDetails.getEmail().equalsIgnoreCase(user.getEmail())) {
            throw new IllegalArgumentException(
                    "This account is not registered under your email");
        }

        if (Boolean.TRUE.equals(bankDetails.getRegistered())) {
            throw new IllegalArgumentException(
                    "This bank account has already been linked");
        }

        // Create the money-transfer account seeded from the verified bank record
        Account account = Account.builder()
                .id(accountNumber)
                .holderName(bankDetails.getUserName())
                .balance(bankDetails.getBalance())
                .status(AccountStatus.ACTIVE)
                .version(0)
                .build();
        Account savedAccount = accountRepository.save(account);

        user.setAccountId(savedAccount.getId());
        user.setName(bankDetails.getUserName());
        userRepository.save(user);

        bankDetails.setRegistered(true);
        bankDetailsRepository.save(bankDetails);

        log.info("Bank account {} linked to user {}", accountNumber, username);

        return LinkBankResponse.builder()
                .accountId(savedAccount.getId())
                .holderName(savedAccount.getHolderName())
                .balance(savedAccount.getBalance())
                .message("Bank account linked successfully")
                .build();
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
