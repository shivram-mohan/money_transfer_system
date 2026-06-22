package com.fidelity.moneytransfer.service;

import com.fidelity.moneytransfer.config.CryptoService;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceExtraTest {

    @Mock private UserRepository userRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private BankDetailsRepository bankDetailsRepository;
    @Mock private CryptoService cryptoService;
    @InjectMocks private UserService userService;

    private AppUser user;

    @BeforeEach
    void setUp() {
        user = AppUser.builder().id(1L).username("john").password("enc").name("John")
                .email("john@example.com").role("USER").status("ACTIVE").accountId(100L)
                .build();
    }

    @Test
    void getUserById_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        UserResponseDto dto = userService.getUserById(1L);
        assertEquals("john", dto.getUsername());
    }

    @Test
    void getUserByUsername_success() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        assertEquals("john", userService.getUserByUsername("john").getUsername());
    }

    @Test
    void getUserByUsername_notFound_throws() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        assertThrows(AccountNotFoundException.class, () -> userService.getUserByUsername("ghost"));
    }

    @Test
    void completeSignup_duplicateEmail_throws() {
        SetPasswordRequest req = new SetPasswordRequest("newuser", "dupe@example.com", "New", "pw");
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("dupe@example.com")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> userService.completeSignup(req));
        verify(userRepository, never()).save(any());
    }

    @Test
    void getUserForPasswordReset_notFound_throws() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        assertThrows(AccountNotFoundException.class, () -> userService.getUserForPasswordReset("ghost"));
    }

    @Test
    void getUserForPasswordReset_found() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        assertEquals("john@example.com", userService.getUserForPasswordReset("john").getEmail());
    }

    @Test
    void resetPassword_success() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpw")).thenReturn("bcrypt");
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));
        userService.resetPassword("john", "newpw");
        assertEquals("bcrypt", user.getPassword());
    }

    @Test
    void resetPassword_userNotFound_throws() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        assertThrows(AccountNotFoundException.class, () -> userService.resetPassword("ghost", "x"));
    }

    @Test
    void linkBankAccount_userNotFound_throws() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        assertThrows(AccountNotFoundException.class, () -> userService.linkBankAccount("ghost", 1L));
    }

    @Test
    void linkBankAccount_accountNumberNotFound_throws() {
        AppUser unlinked = AppUser.builder().id(2L).username("amy").name("amy")
                .email("amy@example.com").role("USER").status("ACTIVE").accountId(null).build();
        when(userRepository.findByUsername("amy")).thenReturn(Optional.of(unlinked));
        when(bankDetailsRepository.findByAccountNumber(999L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> userService.linkBankAccount("amy", 999L));
    }

    @Test
    void linkBankAccount_alreadyRegistered_throws() {
        AppUser unlinked = AppUser.builder().id(2L).username("amy").name("amy")
                .email("amy@example.com").role("USER").status("ACTIVE").accountId(null).build();
        BankDetails bank = BankDetails.builder().accountNumber(500L).userName("Amy")
                .email("amy@example.com").balance(new BigDecimal("10.00")).registered(true).build();
        when(userRepository.findByUsername("amy")).thenReturn(Optional.of(unlinked));
        when(bankDetailsRepository.findByAccountNumber(500L)).thenReturn(Optional.of(bank));
        assertThrows(IllegalArgumentException.class, () -> userService.linkBankAccount("amy", 500L));
    }

    @Test
    void activateUser_success_withLinkedAccount() {
        Account acct = Account.builder().id(100L).holderName("John")
                .balance(BigDecimal.TEN).status(AccountStatus.LOCKED).version(0).build();
        user.setStatus("INACTIVE");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(accountRepository.findById(100L)).thenReturn(Optional.of(acct));
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponseDto dto = userService.activateUser(1L);
        assertEquals("ACTIVE", dto.getStatus());
        assertEquals(AccountStatus.ACTIVE, acct.getStatus());
        verify(accountRepository).save(acct);
    }

    @Test
    void activateUser_noLinkedAccount_skipsAccountUpdate() {
        user.setAccountId(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));
        userService.activateUser(1L);
        verify(accountRepository, never()).findById(any());
    }

    @Test
    void activateUser_notFound_throws() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(AccountNotFoundException.class, () -> userService.activateUser(9L));
    }

    @Test
    void deactivateUser_noLinkedAccount_skipsAccountUpdate() {
        user.setAccountId(null);
        DeactivateUserRequest req = new DeactivateUserRequest(1L, "reason");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));
        UserResponseDto dto = userService.deactivateUser(req);
        assertEquals("INACTIVE", dto.getStatus());
        verify(accountRepository, never()).findById(any());
    }

    @Test
    void deactivateUser_notFound_throws() {
        DeactivateUserRequest req = new DeactivateUserRequest(9L, "reason");
        when(userRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(AccountNotFoundException.class, () -> userService.deactivateUser(req));
    }
}
