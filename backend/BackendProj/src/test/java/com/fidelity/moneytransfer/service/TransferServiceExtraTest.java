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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
        from = Account.builder().id(1L).holderName("John")
                .balance(new BigDecimal("1000.00"))
                .status(AccountStatus.ACTIVE).version(0).build();
        to = Account.builder().id(2L).holderName("Jane")
                .balance(new BigDecimal("500.00"))
                .status(AccountStatus.ACTIVE).version(0).build();
        request = TransferRequest.builder()
                .fromAccountId(1L).toAccountId(2L)
                .amount(new BigDecimal("200.00")).idempotencyKey("k").build();

        when(transactionLogRepository.findByIdempotencyKey(anyString()))
                .thenReturn(Optional.empty());
        when(accountService.getAccountById(1L)).thenReturn(from);
        when(accountService.getAccountById(2L)).thenReturn(to);
        when(accountRepository.save(any(Account.class)))
                .thenAnswer(i -> i.getArgument(0));
        when(transactionLogRepository.save(any(TransactionLog.class)))
                .thenAnswer(i -> {
                    TransactionLog t = i.getArgument(0);
                    if (t.getId() == null) t.setId("txn-id");
                    return t;
                });
    }

    @Test
    void transfer_SyncsBankDetails_AndAttachesReward() {
        when(bankDetailsRepository.findByAccountNumber(1L))
                .thenReturn(Optional.of(BankDetails.builder()
                        .accountNumber(1L).userName("John").email("j@x.com")
                        .balance(new BigDecimal("1000.00")).registered(true).build()));
        when(bankDetailsRepository.findByAccountNumber(2L))
                .thenReturn(Optional.empty()); // admin account, skipped
        RewardResult reward = RewardResult.builder()
                .rewarded(true).pointsEarned(2L).totalPoints(2L).build();
        when(rewardService.processReward(any(), anyString())).thenReturn(reward);

        TransferResponse response = transferService.transfer(request);

        assertEquals("SUCCESS", response.getStatus());
        assertSame(reward, response.getReward());
        verify(bankDetailsRepository).save(any(BankDetails.class)); // only the linked one
    }

    @Test
    void transfer_RewardFailure_IsSwallowed_TransferStillSucceeds() {
        when(rewardService.processReward(any(), anyString()))
                .thenThrow(new RuntimeException("reward subsystem down"));

        TransferResponse response = transferService.transfer(request);

        assertEquals("SUCCESS", response.getStatus());
        assertNull(response.getReward());
    }

    @Test
    void transfer_ToCashbackAccount_Blocked() {
        request.setToAccountId(RewardConstants.CASHBACK_ACCOUNT_ID);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> transferService.transfer(request));
        assertTrue(ex.getMessage().contains("not allowed"));
        verify(transactionLogService).logFailedTransfer(any(), anyString());
    }

    @Test
    void transfer_NullAmount_Rejected() {
        request.setAmount(null);
        assertThrows(IllegalArgumentException.class,
                () -> transferService.transfer(request));
    }

    @Test
    void transfer_NullToAccountId_SkipsCashbackGuard_ThenFailsLookup() {
        // toAccountId null exercises the "!= null" false side of the cashback guard
        request.setToAccountId(null);
        when(accountService.getAccountById(null))
                .thenThrow(new com.fidelity.moneytransfer.exception.AccountNotFoundException("null"));

        assertThrows(com.fidelity.moneytransfer.exception.AccountNotFoundException.class,
                () -> transferService.transfer(request));
        verify(transactionLogService).logFailedTransfer(any(), anyString());
    }
}
