package com.fidelity.moneytransfer.service;

import com.fidelity.moneytransfer.dto.SignupRequest;
import com.fidelity.moneytransfer.dto.UserResponseDto;
import com.fidelity.moneytransfer.entity.Account;
import com.fidelity.moneytransfer.entity.AppUser;
import com.fidelity.moneytransfer.enums.AccountStatus;
import com.fidelity.moneytransfer.exception.AccountNotFoundException;
import com.fidelity.moneytransfer.repository.AccountRepository;
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
                .status(AccountStatus.LOCKED)
                .build();

        testUser = AppUser.builder()
                .id(1L)
                .username("testuser")
                .password("encoded-password")
                .name("Test User")
                .email("test@example.com")
                .role("USER")
                .status("PENDING")
                .accountId(1L)
                .build();
    }

    @Test
    void getAllUsers_Success() {
        // Arrange
        AppUser user2 = AppUser.builder()
                .id(2L)
                .username("user2")
                .name("User Two")
                .role("USER")
                .status("ACTIVE")
                .build();

        when(userRepository.findAll())
                .thenReturn(Arrays.asList(testUser, user2));

        // Act
        List<UserResponseDto> users = userService.getAllUsers();

        // Assert
        assertEquals(2, users.size());
        assertEquals("testuser", users.get(0).getUsername());
        assertEquals("user2", users.get(1).getUsername());
    }

    @Test
    void getPendingUsers_Success() {
        // Arrange
        when(userRepository.findByStatus("PENDING"))
                .thenReturn(Arrays.asList(testUser));

        // Act
        List<UserResponseDto> pendingUsers = userService.getPendingUsers();

        // Assert
        assertEquals(1, pendingUsers.size());
        assertEquals("PENDING", pendingUsers.get(0).getStatus());
    }

    @Test
    void signupUser_Success() {
        // Arrange
        SignupRequest request = SignupRequest.builder()
                .username("newuser")
                .password("password123")
                .name("New User")
                .email("new@example.com")
                .initialBalance(new BigDecimal("5000.00"))
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-pwd");
        when(accountRepository.save(any(Account.class)))
                .thenAnswer(inv -> {
                    Account acc = inv.getArgument(0);
                    acc.setId(10L);
                    return acc;
                });
        when(userRepository.save(any(AppUser.class)))
                .thenAnswer(inv -> {
                    AppUser user = inv.getArgument(0);
                    user.setId(10L);
                    return user;
                });

        // Act
        UserResponseDto response = userService.signupUser(request);

        // Assert
        assertNotNull(response);
        assertEquals("newuser", response.getUsername());
        assertEquals("PENDING", response.getStatus());
        verify(accountRepository, times(1)).save(any(Account.class));
        verify(userRepository, times(1)).save(any(AppUser.class));
    }

    @Test
    void signupUser_DuplicateUsername_ThrowsException() {
        // Arrange
        SignupRequest request = SignupRequest.builder()
                .username("testuser")
                .password("password123")
                .name("Test")
                .initialBalance(new BigDecimal("1000.00"))
                .build();

        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.signupUser(request);
        });

        verify(accountRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void approveUser_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(userRepository.save(any(AppUser.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(accountRepository.save(any(Account.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Act
        UserResponseDto response = userService.approveUser(1L, "admin");

        // Assert
        assertEquals("ACTIVE", response.getStatus());
        assertEquals("ACTIVE", testUser.getStatus());
        assertEquals(AccountStatus.ACTIVE, testAccount.getStatus());
        assertNotNull(testUser.getApprovedBy());
        verify(userRepository, times(1)).save(testUser);
        verify(accountRepository, times(1)).save(testAccount);
    }

    @Test
    void approveUser_NotPending_ThrowsException() {
        // Arrange
        testUser.setStatus("ACTIVE");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.approveUser(1L, "admin");
        });
    }

    @Test
    void rejectUser_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        userService.rejectUser(1L, "Not eligible");

        // Assert
        verify(accountRepository, times(1)).deleteById(1L);
        verify(userRepository, times(1)).deleteById(1L);
    }

    @Test
    void getUserById_NotFound_ThrowsException() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(AccountNotFoundException.class, () -> {
            userService.getUserById(999L);
        });
    }
}