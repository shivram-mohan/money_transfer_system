package com.fidelity.moneytransfer.service;

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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccountServiceExtraTest {

    @Mock private AccountRepository accountRepository;
    @Mock private TransactionLogRepository transactionLogRepository;
    @InjectMocks private AccountService accountService;

    private Account own;
    private Account counterparty;

    @BeforeEach
    void setUp() {
        own = Account.builder().id(1001L).holderName("John")
                .balance(new BigDecimal("100.00"))
                .status(AccountStatus.ACTIVE).version(0).build();
        counterparty = Account.builder().id(2002L).holderName("Jane")
                .balance(new BigDecimal("100.00"))
                .status(AccountStatus.ACTIVE).version(0).build();
        when(accountRepository.findById(1001L)).thenReturn(Optional.of(own));
        when(accountRepository.findById(2002L)).thenReturn(Optional.of(counterparty));
        when(accountRepository.findById(RewardConstants.CASHBACK_ACCOUNT_ID))
                .thenReturn(Optional.empty());
    }

    @Test
    void generateUniqueAccountId_RetriesUntilUnused() {
        when(accountRepository.existsById(anyLong()))
                .thenReturn(true)   // first generated id collides
                .thenReturn(false); // second is free

        Long id = accountService.generateUniqueAccountId();

        assertTrue(id >= 1_000_000_000L && id < 10_000_000_000L);
        verify(accountRepository, times(2)).existsById(anyLong());
    }

    @Test
    void getTransactionHistory_PopulatesNames_AndMasksCashback() {
        TransactionLog normal = TransactionLog.builder()
                .id("t1").fromAccountId(2002L).toAccountId(1001L)
                .amount(new BigDecimal("50.00")).status(TransactionStatus.SUCCESS)
                .createdOn(LocalDateTime.now()).build();
        TransactionLog cashback = TransactionLog.builder()
                .id("t2").fromAccountId(RewardConstants.CASHBACK_ACCOUNT_ID)
                .toAccountId(1001L)
                .amount(new BigDecimal("500.00")).status(TransactionStatus.SUCCESS)
                .createdOn(LocalDateTime.now()).build();

        when(transactionLogRepository.findVisibleByAccountId(1001L))
                .thenReturn(List.of(normal, cashback));

        List<TransactionLog> result = accountService.getTransactionHistory(1001L);

        assertEquals(2, result.size());
        assertEquals("Jane", result.get(0).getFromAccountHolderName());
        assertEquals("John", result.get(0).getToAccountHolderName());
        // cashback source masked
        assertNull(result.get(1).getFromAccountId());
        assertEquals(RewardConstants.CASHBACK_ACCOUNT_NAME,
                result.get(1).getFromAccountHolderName());
    }

    @Test
    void getFilteredTransactionHistory_MasksCashbackToAccount() {
        TransactionLog toCashback = TransactionLog.builder()
                .id("t3").fromAccountId(1001L)
                .toAccountId(RewardConstants.CASHBACK_ACCOUNT_ID)
                .amount(new BigDecimal("10.00")).status(TransactionStatus.SUCCESS)
                .createdOn(LocalDateTime.now()).build();

        when(transactionLogRepository.findByAccountIdAndDateRange(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(toCashback));

        List<TransactionLog> result = accountService.getFilteredTransactionHistory(
                1001L, LocalDate.now().minusDays(7), LocalDate.now());

        assertEquals(1, result.size());
        assertNull(result.get(0).getToAccountId());
        assertEquals(RewardConstants.CASHBACK_ACCOUNT_NAME,
                result.get(0).getToAccountHolderName());
    }

    @Test
    void getFilteredTransactionHistory_PopulatesBothHolderNames() {
        TransactionLog normal = TransactionLog.builder()
                .id("t4").fromAccountId(1001L).toAccountId(2002L)
                .amount(new BigDecimal("25.00")).status(TransactionStatus.SUCCESS)
                .createdOn(LocalDateTime.now()).build();
        when(transactionLogRepository.findByAccountIdAndDateRange(
                anyLong(), any(), any())).thenReturn(List.of(normal));

        List<TransactionLog> result = accountService.getFilteredTransactionHistory(
                1001L, LocalDate.now().minusDays(7), LocalDate.now());

        assertEquals("John", result.get(0).getFromAccountHolderName());
        assertEquals("Jane", result.get(0).getToAccountHolderName());
    }

    @Test
    void getFilteredTransactionHistory_NullAccountIds_SkipNamePopulation() {
        TransactionLog nullIds = TransactionLog.builder()
                .id("t5").fromAccountId(null).toAccountId(null)
                .amount(new BigDecimal("1.00")).status(TransactionStatus.SUCCESS)
                .createdOn(LocalDateTime.now()).build();
        when(transactionLogRepository.findByAccountIdAndDateRange(
                anyLong(), any(), any())).thenReturn(List.of(nullIds));

        List<TransactionLog> result = accountService.getFilteredTransactionHistory(
                1001L, LocalDate.now().minusDays(7), LocalDate.now());

        assertNull(result.get(0).getFromAccountHolderName());
        assertNull(result.get(0).getToAccountHolderName());
    }

    @Test
    void lastWeekMonthYear_DelegateToFilter() {
        when(transactionLogRepository.findByAccountIdAndDateRange(
                anyLong(), any(), any())).thenReturn(List.of());

        assertNotNull(accountService.getLastWeekTransactions(1001L));
        assertNotNull(accountService.getLastMonthTransactions(1001L));
        assertNotNull(accountService.getLastYearTransactions(1001L));
        verify(transactionLogRepository, times(3))
                .findByAccountIdAndDateRange(anyLong(), any(), any());
    }
}
