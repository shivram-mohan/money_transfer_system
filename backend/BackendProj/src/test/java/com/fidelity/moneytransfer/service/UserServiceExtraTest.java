package com.fidelity.moneytransfer.service;

import com.fidelity.moneytransfer.dto.SetPasswordRequest;
import com.fidelity.moneytransfer.dto.UserResponseDto;
import com.fidelity.moneytransfer.entity.AppUser;
import com.fidelity.moneytransfer.entity.BankDetails;
import com.fidelity.moneytransfer.exception.AccountNotFoundException;
import com.fidelity.moneytransfer.repository.AccountRepository;
import com.fidelity.moneytransfer.repository.BankDetailsRepository;
import com.fidelity.moneytransfer.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceExtraTest {

    @Mock private UserRepository userRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private BankDetailsRepository bankDetailsRepository;
    @InjectMocks private UserService userService;

    private AppUser user;

    @BeforeEach
    void setUp() {
        user = AppUser.builder().id(1L).username("john").password("hash")
                .name("John").email("john@example.com")
                .role("USER").status("ACTIVE").accountId(5L).build();
        when(userRepository.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));
        when(accountRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void getUserById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        UserResponseDto dto = userService.getUserById(1L);
        assertEquals("john", dto.getUsername());
    }

    @Test
    void getUserByUsername_Success() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        assertEquals("John", userService.getUserByUsername("john").getName());
    }

    @Test
    void getUserByUsername_NotFound_Throws() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        assertThrows(AccountNotFoundException.class,
                () -> userService.getUserByUsername("ghost"));
    }

    @Test
    void completeSignup_DuplicateEmail_Throws() {
        SetPasswordRequest req = new SetPasswordRequest(
                "newuser", "john@example.com", "New", "pw");
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("john@example.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> userService.completeSignup(req));
    }

    @Test
    void getUserForPasswordReset_Success_AndNotFound() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        assertEquals(user, userService.getUserForPasswordReset("john"));

        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        assertThrows(AccountNotFoundException.class,
                () -> userService.getUserForPasswordReset("ghost"));
    }

    @Test
    void resetPassword_Success() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpw")).thenReturn("newhash");

        userService.resetPassword("john", "newpw");

        assertEquals("newhash", user.getPassword());
        verify(userRepository).save(user);
    }

    @Test
    void resetPassword_UserNotFound_Throws() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        assertThrows(AccountNotFoundException.class,
                () -> userService.resetPassword("ghost", "x"));
    }

    @Test
    void linkBankAccount_UserNotFound_Throws() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        assertThrows(AccountNotFoundException.class,
                () -> userService.linkBankAccount("ghost", 1001L));
    }

    @Test
    void linkBankAccount_AccountNumberNotFound_Throws() {
        AppUser unlinked = AppUser.builder().id(2L).username("amy")
                .email("amy@example.com").role("USER").status("ACTIVE")
                .accountId(null).build();
        when(userRepository.findByUsername("amy")).thenReturn(Optional.of(unlinked));
        when(bankDetailsRepository.findByAccountNumber(9L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> userService.linkBankAccount("amy", 9L));
    }

    @Test
    void linkBankAccount_AlreadyRegistered_Throws() {
        AppUser unlinked = AppUser.builder().id(2L).username("amy")
                .email("amy@example.com").role("USER").status("ACTIVE")
                .accountId(null).build();
        BankDetails details = BankDetails.builder()
                .accountNumber(9L).userName("Amy").email("amy@example.com")
                .balance(new BigDecimal("100.00")).registered(true).build();
        when(userRepository.findByUsername("amy")).thenReturn(Optional.of(unlinked));
        when(bankDetailsRepository.findByAccountNumber(9L)).thenReturn(Optional.of(details));

        assertThrows(IllegalArgumentException.class,
                () -> userService.linkBankAccount("amy", 9L));
    }

    @Test
    void activateUser_Success_WithLinkedAccount() {
        com.fidelity.moneytransfer.entity.Account acct =
                com.fidelity.moneytransfer.entity.Account.builder()
                        .id(5L).holderName("John")
                        .balance(new BigDecimal("10.00"))
                        .status(com.fidelity.moneytransfer.enums.AccountStatus.LOCKED)
                        .version(0).build();
        user.setStatus("INACTIVE");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(accountRepository.findById(5L)).thenReturn(Optional.of(acct));

        UserResponseDto dto = userService.activateUser(1L);

        assertEquals("ACTIVE", dto.getStatus());
        assertEquals(com.fidelity.moneytransfer.enums.AccountStatus.ACTIVE,
                acct.getStatus());
    }

    @Test
    void activateUser_NoLinkedAccount_StillActivatesUser() {
        user.setAccountId(null);
        user.setStatus("INACTIVE");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponseDto dto = userService.activateUser(1L);

        assertEquals("ACTIVE", dto.getStatus());
        verify(accountRepository, never()).findById(any());
    }

    @Test
    void activateUser_NotFound_Throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(AccountNotFoundException.class,
                () -> userService.activateUser(99L));
    }

    @Test
    void deactivateUser_NotFound_Throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(AccountNotFoundException.class, () -> userService.deactivateUser(
                new com.fidelity.moneytransfer.dto.DeactivateUserRequest(99L, "x")));
    }

    @Test
    void deactivateUser_NoLinkedAccount_StillDeactivatesUser() {
        user.setAccountId(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponseDto dto = userService.deactivateUser(
                new com.fidelity.moneytransfer.dto.DeactivateUserRequest(1L, "x"));

        assertEquals("INACTIVE", dto.getStatus());
        verify(accountRepository, never()).findById(any());
    }
}
