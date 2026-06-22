package com.fidelity.moneytransfer.service;

import com.fidelity.moneytransfer.config.CryptoService;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private BankDetailsRepository bankDetailsRepository;

    @Mock
    private CryptoService cryptoService;

    @InjectMocks
    private UserService userService;

    private AppUser testUser;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        testAccount = Account.builder()
                .id(1L)
                .holderName("Test User")
                .balance(new BigDecimal("1000.00"))
                .status(AccountStatus.ACTIVE)
                .build();

        testUser = AppUser.builder()
                .id(1L)
                .username("testuser")
                .password("encoded-password")
                .name("Test User")
                .email("test@example.com")
                .role("USER")
                .status("ACTIVE")
                .accountId(1L)
                .build();
    }

    @Test
    void getAllUsers_Success() {
        AppUser user2 = AppUser.builder()
                .id(2L)
                .username("user2")
                .name("User Two")
                .role("USER")
                .status("ACTIVE")
                .build();

        when(userRepository.findAll())
                .thenReturn(Arrays.asList(testUser, user2));

        List<UserResponseDto> users = userService.getAllUsers();

        assertEquals(2, users.size());
        assertEquals("testuser", users.get(0).getUsername());
        assertEquals("user2", users.get(1).getUsername());
    }

    @Test
    void completeSignup_Success_CreatesUserWithoutBankAccount() {
        SetPasswordRequest request = new SetPasswordRequest(
                "newuser", "new@example.com", "New User", "hashed-password");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("hashed-password")).thenReturn("bcrypt-hash");
        when(userRepository.save(any(AppUser.class)))
                .thenAnswer(inv -> {
                    AppUser user = inv.getArgument(0);
                    user.setId(10L);
                    return user;
                });

        UserResponseDto response = userService.completeSignup(request);

        assertNotNull(response);
        assertEquals("newuser", response.getUsername());
        assertEquals("ACTIVE", response.getStatus());
        // No bank account is linked at signup time
        assertNull(response.getAccountId());
        verify(accountRepository, never()).save(any(Account.class));
        verify(userRepository, times(1)).save(any(AppUser.class));
    }

    @Test
    void completeSignup_DuplicateUsername_ThrowsException() {
        SetPasswordRequest request = new SetPasswordRequest(
                "testuser", "test@example.com", "Test", "hashed-password");

        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> userService.completeSignup(request));

        verify(userRepository, never()).save(any());
    }

    @Test
    void linkBankAccount_Success() {
        AppUser unlinkedUser = AppUser.builder()
                .id(5L)
                .username("alice")
                .name("alice")
                .email("alice.brown@example.com")
                .role("USER")
                .status("ACTIVE")
                .accountId(null)
                .build();

        BankDetails bankDetails = BankDetails.builder()
                .accountNumber(1001001004L)
                .userName("Alice Brown")
                .email("alice.brown@example.com")
                .balance(new BigDecimal("30000.00"))
                .registered(false)
                .build();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(unlinkedUser));
        when(bankDetailsRepository.findByAccountNumber(1001001004L))
                .thenReturn(Optional.of(bankDetails));
        when(accountRepository.save(any(Account.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any(AppUser.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(cryptoService.encrypt("30000.00")).thenReturn("ENC(30000.00)");

        LinkBankResponse response = userService.linkBankAccount("alice", 1001001004L);

        assertEquals(1001001004L, response.getAccountId());
        assertEquals("Alice Brown", response.getHolderName());
        // Balance is returned encrypted, not as a plaintext number
        assertEquals("ENC(30000.00)", response.getEncryptedBalance());
        assertEquals(1001001004L, unlinkedUser.getAccountId());
        assertTrue(bankDetails.getRegistered());
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    void linkBankAccount_AlreadyLinked_ThrowsException() {
        // testUser already has accountId = 1L
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        assertThrows(IllegalArgumentException.class,
                () -> userService.linkBankAccount("testuser", 1001001001L));

        verify(accountRepository, never()).save(any());
    }

    @Test
    void linkBankAccount_EmailMismatch_ThrowsException() {
        AppUser unlinkedUser = AppUser.builder()
                .id(5L)
                .username("bob")
                .name("bob")
                .email("bob@example.com")
                .role("USER")
                .status("ACTIVE")
                .accountId(null)
                .build();

        BankDetails bankDetails = BankDetails.builder()
                .accountNumber(1001001001L)
                .userName("John Doe")
                .email("john.doe@example.com")
                .balance(new BigDecimal("50000.00"))
                .registered(false)
                .build();

        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(unlinkedUser));
        when(bankDetailsRepository.findByAccountNumber(1001001001L))
                .thenReturn(Optional.of(bankDetails));

        assertThrows(IllegalArgumentException.class,
                () -> userService.linkBankAccount("bob", 1001001001L));

        verify(accountRepository, never()).save(any());
    }

    @Test
    void deactivateUser_Success() {
        DeactivateUserRequest request = new DeactivateUserRequest(1L, "Policy violation");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(userRepository.save(any(AppUser.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(accountRepository.save(any(Account.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UserResponseDto response = userService.deactivateUser(request);

        assertEquals("INACTIVE", response.getStatus());
        assertEquals("INACTIVE", testUser.getStatus());
        assertEquals(AccountStatus.LOCKED, testAccount.getStatus());
    }

    @Test
    void getUserById_NotFound_ThrowsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> userService.getUserById(999L));
    }
}
