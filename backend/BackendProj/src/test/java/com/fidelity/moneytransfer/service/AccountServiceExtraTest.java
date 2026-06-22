package com.fidelity.moneytransfer.service;

import com.fidelity.moneytransfer.config.CryptoService;
import com.fidelity.moneytransfer.constants.RewardConstants;
import com.fidelity.moneytransfer.entity.Account;
import com.fidelity.moneytransfer.entity.TransactionLog;
import com.fidelity.moneytransfer.enums.AccountStatus;
import com.fidelity.moneytransfer.enums.TransactionStatus;
import com.fidelity.moneytransfer.repository.AccountRepository;
import com.fidelity.moneytransfer.repository.TransactionLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceExtraTest {

    @Mock private AccountRepository accountRepository;
    @Mock private TransactionLogRepository transactionLogRepository;
    @Mock private CryptoService cryptoService;
    @InjectMocks private AccountService accountService;

    private Account acct;

    @BeforeEach
    void setUp() {
        acct = Account.builder().id(1L).holderName("Holder")
                .balance(new BigDecimal("100.00")).status(AccountStatus.ACTIVE).version(0).build();
    }

    @Test
    void generateUniqueAccountId_retriesOnCollision() {
        // First generated id collides, second is free.
        when(accountRepository.existsById(anyLong())).thenReturn(true, false);
        Long id = accountService.generateUniqueAccountId();
        assertNotNull(id);
        verify(accountRepository, times(2)).existsById(anyLong());
    }

    @Test
    void getTransactionHistory_populatesHolderNames_andMasksCashback() {
        Account counter = Account.builder().id(2L).holderName("Counter")
                .balance(BigDecimal.TEN).status(AccountStatus.ACTIVE).build();
        // Normal txn: 1 -> 2
        TransactionLog normal = TransactionLog.builder()
                .id("t1").fromAccountId(1L).toAccountId(2L)
                .amount(BigDecimal.TEN).status(TransactionStatus.SUCCESS)
                .createdOn(LocalDateTime.now()).build();
        // Cashback credit: CASHBACK -> 1 (fromAccountId is the cashback account)
        TransactionLog cashback = TransactionLog.builder()
                .id("t2").fromAccountId(RewardConstants.CASHBACK_ACCOUNT_ID).toAccountId(1L)
                .amount(BigDecimal.ONE).status(TransactionStatus.SUCCESS)
                .createdOn(LocalDateTime.now()).build();

        when(accountRepository.findById(1L)).thenReturn(Optional.of(acct));
        when(transactionLogRepository.findVisibleByAccountId(1L))
                .thenReturn(List.of(normal, cashback));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(counter));
        // cashback id lookup -> empty (masking still runs based on id match)
        when(accountRepository.findById(RewardConstants.CASHBACK_ACCOUNT_ID)).thenReturn(Optional.empty());

        List<TransactionLog> result = accountService.getTransactionHistory(1L);

        assertEquals(2, result.size());
        assertEquals("Holder", normal.getFromAccountHolderName());
        assertEquals("Counter", normal.getToAccountHolderName());
        // cashback fromAccountId masked to null with CASHBACK label
        assertNull(cashback.getFromAccountId());
        assertEquals(RewardConstants.CASHBACK_ACCOUNT_NAME, cashback.getFromAccountHolderName());
    }

    @Test
    void getFilteredTransactionHistory_masksCashbackOnToSide() {
        // Transfer INTO cashback account would never happen, but exercise the
        // to-side mask branch defensively: 1 -> CASHBACK.
        TransactionLog toCashback = TransactionLog.builder()
                .id("t3").fromAccountId(1L).toAccountId(RewardConstants.CASHBACK_ACCOUNT_ID)
                .amount(BigDecimal.ONE).status(TransactionStatus.SUCCESS)
                .createdOn(LocalDateTime.now()).build();

        when(accountRepository.findById(1L)).thenReturn(Optional.of(acct));
        when(accountRepository.findById(RewardConstants.CASHBACK_ACCOUNT_ID)).thenReturn(Optional.empty());
        when(transactionLogRepository.findByAccountIdAndDateRange(eq(1L), any(), any()))
                .thenReturn(List.of(toCashback));

        List<TransactionLog> result = accountService.getFilteredTransactionHistory(
                1L, LocalDate.now().minusDays(3), LocalDate.now());

        assertEquals(1, result.size());
        assertNull(toCashback.getToAccountId());
        assertEquals(RewardConstants.CASHBACK_ACCOUNT_NAME, toCashback.getToAccountHolderName());
    }

    @Test
    void getFilteredTransactionHistory_populatesNames_andHandlesNullIds() {
        Account counter = Account.builder().id(2L).holderName("Counter")
                .balance(BigDecimal.TEN).status(AccountStatus.ACTIVE).build();
        // Normal txn 1 -> 2: both holder-name lambdas run.
        TransactionLog normal = TransactionLog.builder()
                .id("n1").fromAccountId(1L).toAccountId(2L)
                .amount(BigDecimal.TEN).status(TransactionStatus.SUCCESS)
                .createdOn(LocalDateTime.now()).build();
        // Txn with null ids: the != null guards take their false branch.
        TransactionLog nullIds = TransactionLog.builder()
                .id("n2").fromAccountId(null).toAccountId(null)
                .amount(BigDecimal.ONE).status(TransactionStatus.SUCCESS)
                .createdOn(LocalDateTime.now()).build();

        when(accountRepository.findById(1L)).thenReturn(Optional.of(acct));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(counter));
        when(transactionLogRepository.findByAccountIdAndDateRange(eq(1L), any(), any()))
                .thenReturn(List.of(normal, nullIds));

        List<TransactionLog> result = accountService.getFilteredTransactionHistory(
                1L, LocalDate.now().minusDays(3), LocalDate.now());

        assertEquals(2, result.size());
        assertEquals("Holder", normal.getFromAccountHolderName());
        assertEquals("Counter", normal.getToAccountHolderName());
        assertNull(nullIds.getFromAccountHolderName());
    }

    @Test
    void getLastWeekTransactions_delegatesToFilter() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(acct));
        when(transactionLogRepository.findByAccountIdAndDateRange(eq(1L), any(), any()))
                .thenReturn(List.of());
        assertTrue(accountService.getLastWeekTransactions(1L).isEmpty());
    }

    @Test
    void getLastMonthTransactions_delegatesToFilter() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(acct));
        when(transactionLogRepository.findByAccountIdAndDateRange(eq(1L), any(), any()))
                .thenReturn(List.of());
        assertTrue(accountService.getLastMonthTransactions(1L).isEmpty());
    }

    @Test
    void getLastYearTransactions_delegatesToFilter() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(acct));
        when(transactionLogRepository.findByAccountIdAndDateRange(eq(1L), any(), any()))
                .thenReturn(List.of());
        assertTrue(accountService.getLastYearTransactions(1L).isEmpty());
    }
}
