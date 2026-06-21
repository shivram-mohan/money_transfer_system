package com.fidelity.moneytransfer.service;

import com.fidelity.moneytransfer.dto.TransferRequest;
import com.fidelity.moneytransfer.dto.TransferResponse;
import com.fidelity.moneytransfer.entity.Account;
import com.fidelity.moneytransfer.entity.TransactionLog;
import com.fidelity.moneytransfer.enums.AccountStatus;
import com.fidelity.moneytransfer.exception.AccountNotActiveException;
import com.fidelity.moneytransfer.exception.AccountNotFoundException;
import com.fidelity.moneytransfer.exception.DuplicateTransferException;
import com.fidelity.moneytransfer.exception.InsufficientBalanceException;
import com.fidelity.moneytransfer.repository.AccountRepository;
import com.fidelity.moneytransfer.repository.BankDetailsRepository;
import com.fidelity.moneytransfer.repository.TransactionLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private AccountService accountService;  // ✅ Mock AccountService instead

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionLogRepository transactionLogRepository;

    @Mock
    private BankDetailsRepository bankDetailsRepository;

    @Mock
    private TransactionLogService transactionLogService;

    @Mock
    private RewardService rewardService;

    @InjectMocks
    private TransferService transferService;

    private Account fromAccount;
    private Account toAccount;
    private TransferRequest transferRequest;

    @BeforeEach
    void setUp() {
        fromAccount = Account.builder()
                .id(1L)
                .holderName("John Doe")
                .balance(new BigDecimal("1000.00"))
                .status(AccountStatus.ACTIVE)
                .version(0)
                .build();

        toAccount = Account.builder()
                .id(2L)
                .holderName("Jane Smith")
                .balance(new BigDecimal("500.00"))
                .status(AccountStatus.ACTIVE)
                .version(0)
                .build();

        transferRequest = TransferRequest.builder()
                .fromAccountId(1L)
                .toAccountId(2L)
                .amount(new BigDecimal("200.00"))
                .idempotencyKey("test-key-123")
                .build();
    }

    @Test
    void transfer_Success() {
        // Arrange
        when(transactionLogRepository.findByIdempotencyKey("test-key-123"))
                .thenReturn(Optional.empty());
        when(accountService.getAccountById(1L)).thenReturn(fromAccount);
        when(accountService.getAccountById(2L)).thenReturn(toAccount);
        when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionLogRepository.save(any(TransactionLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        TransferResponse response = transferService.transfer(transferRequest);

        // Assert
        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals(new BigDecimal("200.00"), response.getAmount());
        assertEquals(1L, response.getDebitedFrom());
        assertEquals(2L, response.getCreditedTo());

        // Verify balances updated
        assertEquals(new BigDecimal("800.00"), fromAccount.getBalance());
        assertEquals(new BigDecimal("700.00"), toAccount.getBalance());

        verify(accountRepository, times(2)).save(any(Account.class));
        verify(transactionLogRepository, times(1)).save(any(TransactionLog.class));
    }

    @Test
    void transfer_DuplicateIdempotencyKey_ThrowsException() {
        // Arrange
        TransactionLog existingTransaction = new TransactionLog();
        when(transactionLogRepository.findByIdempotencyKey("test-key-123"))
                .thenReturn(Optional.of(existingTransaction));

        // Act & Assert
        assertThrows(DuplicateTransferException.class, () -> {
            transferService.transfer(transferRequest);
        });

        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void transfer_InsufficientBalance_ThrowsException() {
        // Arrange
        transferRequest.setAmount(new BigDecimal("1500.00"));
        when(transactionLogRepository.findByIdempotencyKey(any()))
                .thenReturn(Optional.empty());
        when(accountService.getAccountById(1L)).thenReturn(fromAccount);
        when(accountService.getAccountById(2L)).thenReturn(toAccount);

        // Act & Assert
        InsufficientBalanceException exception = assertThrows(
                InsufficientBalanceException.class,
                () -> transferService.transfer(transferRequest)
        );

        assertTrue(exception.getMessage().contains("Insufficient balance"));
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void transfer_FromAccountNotFound_ThrowsException() {
        // Arrange
        when(transactionLogRepository.findByIdempotencyKey(any()))
                .thenReturn(Optional.empty());
        when(accountService.getAccountById(1L))
                .thenThrow(new AccountNotFoundException("Account not found: 1"));

        // Act & Assert
        assertThrows(AccountNotFoundException.class, () -> {
            transferService.transfer(transferRequest);
        });
    }

    @Test
    void transfer_ToAccountNotFound_ThrowsException() {
        // Arrange
        when(transactionLogRepository.findByIdempotencyKey(any()))
                .thenReturn(Optional.empty());
        when(accountService.getAccountById(1L)).thenReturn(fromAccount);
        when(accountService.getAccountById(2L))
                .thenThrow(new AccountNotFoundException("Account not found: 2"));

        // Act & Assert
        assertThrows(AccountNotFoundException.class, () -> {
            transferService.transfer(transferRequest);
        });
    }

    @Test
    void transfer_FromAccountNotActive_ThrowsException() {
        // Arrange
        fromAccount.setStatus(AccountStatus.LOCKED);
        when(transactionLogRepository.findByIdempotencyKey(any()))
                .thenReturn(Optional.empty());
        when(accountService.getAccountById(1L)).thenReturn(fromAccount);

        // Act & Assert
        AccountNotActiveException exception = assertThrows(
                AccountNotActiveException.class,
                () -> transferService.transfer(transferRequest)
        );

        assertTrue(exception.getMessage().contains("not active"));
    }

    @Test
    void transfer_ToAccountNotActive_ThrowsException() {
        // Arrange
        toAccount.setStatus(AccountStatus.CLOSED);
        when(transactionLogRepository.findByIdempotencyKey(any()))
                .thenReturn(Optional.empty());
        when(accountService.getAccountById(1L)).thenReturn(fromAccount);
        when(accountService.getAccountById(2L)).thenReturn(toAccount);

        // Act & Assert
        AccountNotActiveException exception = assertThrows(
                AccountNotActiveException.class,
                () -> transferService.transfer(transferRequest)
        );

        assertTrue(exception.getMessage().contains("not active"));
    }

    @Test
    void transfer_SameAccount_ThrowsException() {
        // Arrange
        transferRequest.setToAccountId(1L);
        when(transactionLogRepository.findByIdempotencyKey(any()))
                .thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transferService.transfer(transferRequest)
        );

        assertTrue(exception.getMessage().contains("Source and destination accounts must be different"));
    }

    @Test
    void transfer_NegativeAmount_ThrowsException() {
        // Arrange
        transferRequest.setAmount(new BigDecimal("-100.00"));
        when(transactionLogRepository.findByIdempotencyKey(any()))
                .thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transferService.transfer(transferRequest)
        );

        assertTrue(exception.getMessage().contains("positive"));
    }

    @Test
    void transfer_ZeroAmount_ThrowsException() {
        // Arrange
        transferRequest.setAmount(BigDecimal.ZERO);
        when(transactionLogRepository.findByIdempotencyKey(any()))
                .thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transferService.transfer(transferRequest)
        );

        assertTrue(exception.getMessage().contains("positive"));
    }
}