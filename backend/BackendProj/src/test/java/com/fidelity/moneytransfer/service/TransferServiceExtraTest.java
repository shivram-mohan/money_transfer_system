package com.fidelity.moneytransfer.service;

import com.fidelity.moneytransfer.constants.RewardConstants;
import com.fidelity.moneytransfer.dto.RewardResult;
import com.fidelity.moneytransfer.dto.TransferRequest;
import com.fidelity.moneytransfer.dto.TransferResponse;
import com.fidelity.moneytransfer.entity.Account;
import com.fidelity.moneytransfer.entity.BankDetails;
import com.fidelity.moneytransfer.entity.TransactionLog;
import com.fidelity.moneytransfer.enums.AccountStatus;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceExtraTest {

    @Mock private AccountService accountService;
    @Mock private AccountRepository accountRepository;
    @Mock private TransactionLogRepository transactionLogRepository;
    @Mock private BankDetailsRepository bankDetailsRepository;
    @Mock private TransactionLogService transactionLogService;
    @Mock private RewardService rewardService;
    @InjectMocks private TransferService transferService;

    private Account from;
    private Account to;
    private TransferRequest request;

    @BeforeEach
    void setUp() {
        from = Account.builder().id(1L).holderName("A").balance(new BigDecimal("1000.00"))
                .status(AccountStatus.ACTIVE).version(0).build();
        to = Account.builder().id(2L).holderName("B").balance(new BigDecimal("500.00"))
                .status(AccountStatus.ACTIVE).version(0).build();
        request = TransferRequest.builder().fromAccountId(1L).toAccountId(2L)
                .amount(new BigDecimal("200.00")).idempotencyKey("k").build();
    }

    @Test
    void transfer_toCashbackAccount_blocked() {
        request.setToAccountId(RewardConstants.CASHBACK_ACCOUNT_ID);
        when(transactionLogRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> transferService.transfer(request));
        assertTrue(ex.getMessage().contains("not allowed"));
        verify(transactionLogService).logFailedTransfer(any(), anyString());
    }

    @Test
    void transfer_nullAmount_rejected() {
        request.setAmount(null);
        when(transactionLogRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> transferService.transfer(request));
        assertTrue(ex.getMessage().contains("positive"));
    }

    @Test
    void transfer_nullToAccountId_skipsCashbackCheck() {
        // toAccountId null: the cashback-block guard's null branch is exercised,
        // then the self-transfer check fails (from != null vs null) and the
        // destination account lookup proceeds and is reported not found.
        request.setToAccountId(null);
        when(transactionLogRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(accountService.getAccountById(1L)).thenReturn(from);
        when(accountService.getAccountById(null))
                .thenThrow(new com.fidelity.moneytransfer.exception.AccountNotFoundException("not found"));

        assertThrows(com.fidelity.moneytransfer.exception.AccountNotFoundException.class,
                () -> transferService.transfer(request));
    }

    @Test
    void transfer_attachesRewardResult() {
        RewardResult reward = RewardResult.builder().rewarded(true).pointsEarned(2L)
                .message("You earned 2 reward points.").build();

        when(transactionLogRepository.findByIdempotencyKey("k")).thenReturn(Optional.empty());
        when(accountService.getAccountById(1L)).thenReturn(from);
        when(accountService.getAccountById(2L)).thenReturn(to);
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionLogRepository.save(any(TransactionLog.class))).thenAnswer(inv -> inv.getArgument(0));
        when(rewardService.processReward(any(), any())).thenReturn(reward);

        TransferResponse resp = transferService.transfer(request);

        assertEquals("SUCCESS", resp.getStatus());
        assertNotNull(resp.getReward());
        assertTrue(resp.getReward().isRewarded());
    }

    @Test
    void transfer_syncsBankDetailsBalancesWhenPresent() {
        BankDetails fromBank = BankDetails.builder().accountNumber(1L).userName("A")
                .email("a@x.com").balance(new BigDecimal("1000.00")).registered(true).build();

        when(transactionLogRepository.findByIdempotencyKey("k")).thenReturn(Optional.empty());
        when(accountService.getAccountById(1L)).thenReturn(from);
        when(accountService.getAccountById(2L)).thenReturn(to);
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionLogRepository.save(any(TransactionLog.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bankDetailsRepository.findByAccountNumber(1L)).thenReturn(Optional.of(fromBank));
        when(bankDetailsRepository.findByAccountNumber(2L)).thenReturn(Optional.empty());

        transferService.transfer(request);

        assertEquals(new BigDecimal("800.00"), fromBank.getBalance());
        verify(bankDetailsRepository).save(fromBank);
    }

    @Test
    void transfer_rewardFailureIsSwallowed() {
        when(transactionLogRepository.findByIdempotencyKey("k")).thenReturn(Optional.empty());
        when(accountService.getAccountById(1L)).thenReturn(from);
        when(accountService.getAccountById(2L)).thenReturn(to);
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionLogRepository.save(any(TransactionLog.class))).thenAnswer(inv -> inv.getArgument(0));
        when(rewardService.processReward(any(), any()))
                .thenThrow(new RuntimeException("reward boom"));

        TransferResponse resp = transferService.transfer(request);

        // Reward failure must not fail the transfer; reward comes back null.
        assertEquals("SUCCESS", resp.getStatus());
        assertNull(resp.getReward());
    }
}
