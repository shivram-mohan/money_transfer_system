package com.fidelity.moneytransfer.service;

import com.fidelity.moneytransfer.dto.AccountResponse;
import com.fidelity.moneytransfer.dto.CreateAccountRequest;
import com.fidelity.moneytransfer.entity.Account;
import com.fidelity.moneytransfer.entity.TransactionLog;
import com.fidelity.moneytransfer.enums.AccountStatus;
import com.fidelity.moneytransfer.exception.AccountNotFoundException;
import com.fidelity.moneytransfer.repository.AccountRepository;
import com.fidelity.moneytransfer.repository.TransactionLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionLogRepository transactionLogRepository;

    @InjectMocks
    private AccountService accountService;

    private Account testAccount;

    @BeforeEach
    void setUp() {
        testAccount = Account.builder()
                .id(1L)
                .holderName("Test User")
                .balance(new BigDecimal("1000.00"))
                .status(AccountStatus.ACTIVE)
                .version(0)
                .build();
    }

    @Test
    void getAccountById_Success() {
        // Arrange
        when(accountRepository.findById(1L))
                .thenReturn(Optional.of(testAccount));

        // Act
        Account result = accountService.getAccountById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test User", result.getHolderName());
        verify(accountRepository, times(1)).findById(1L);
    }

    @Test
    void getAccountById_NotFound_ThrowsException() {
        // Arrange
        when(accountRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(AccountNotFoundException.class, () -> {
            accountService.getAccountById(999L);
        });
    }

    @Test
    void getAccountDetails_Success() {
        // Arrange
        when(accountRepository.findById(1L))
                .thenReturn(Optional.of(testAccount));

        // Act
        AccountResponse response = accountService.getAccountDetails(1L);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Test User", response.getHolderName());
        assertEquals(new BigDecimal("1000.00"), response.getBalance());
        assertEquals("ACTIVE", response.getStatus());
    }

    @Test
    void getBalance_Success() {
        // Arrange
        when(accountRepository.findById(1L))
                .thenReturn(Optional.of(testAccount));

        // Act
        BigDecimal balance = accountService.getBalance(1L);

        // Assert
        assertEquals(new BigDecimal("1000.00"), balance);
    }

    @Test
    void getAllAccounts_Success() {
        // Arrange
        Account account2 = Account.builder()
                .id(2L)
                .holderName("User Two")
                .balance(new BigDecimal("500.00"))
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountRepository.findAll())
                .thenReturn(Arrays.asList(testAccount, account2));

        // Act
        List<AccountResponse> accounts = accountService.getAllAccounts();

        // Assert
        assertEquals(2, accounts.size());
        assertEquals("Test User", accounts.get(0).getHolderName());
        assertEquals("User Two", accounts.get(1).getHolderName());
    }

    @Test
    void createAccount_Success() {
        // Arrange
        CreateAccountRequest request = CreateAccountRequest.builder()
                .holderName("New User")
                .initialBalance(new BigDecimal("2000.00"))
                .build();

        when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation -> {
                    Account acc = invocation.getArgument(0);
                    acc.setId(5L);
                    return acc;
                });

        // Act
        AccountResponse response = accountService.createAccount(request);

        // Assert
        assertNotNull(response);
        assertEquals("New User", response.getHolderName());
        assertEquals(new BigDecimal("2000.00"), response.getBalance());
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    void activateAccount_Success() {
        // Arrange
        testAccount.setStatus(AccountStatus.LOCKED);
        when(accountRepository.findById(1L))
                .thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AccountResponse response = accountService.activateAccount(1L);

        // Assert
        assertEquals("ACTIVE", response.getStatus());
        assertEquals(AccountStatus.ACTIVE, testAccount.getStatus());
        verify(accountRepository, times(1)).save(testAccount);
    }

    @Test
    void deactivateAccount_Success() {
        // Arrange
        when(accountRepository.findById(1L))
                .thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AccountResponse response = accountService.deactivateAccount(1L);

        // Assert
        assertEquals("LOCKED", response.getStatus());
        assertEquals(AccountStatus.LOCKED, testAccount.getStatus());
        verify(accountRepository, times(1)).save(testAccount);
    }

    @Test
    void getTransactionHistory_Success() {
        // Arrange
        TransactionLog txn1 = new TransactionLog();
        TransactionLog txn2 = new TransactionLog();

        when(accountRepository.findById(1L))
                .thenReturn(Optional.of(testAccount));
        when(transactionLogRepository.findByFromAccountIdOrToAccountId(1L, 1L))
                .thenReturn(Arrays.asList(txn1, txn2));

        // Act
        List<TransactionLog> transactions = accountService.getTransactionHistory(1L);

        // Assert
        assertEquals(2, transactions.size());
        verify(transactionLogRepository, times(1))
                .findByFromAccountIdOrToAccountId(1L, 1L);
    }
}