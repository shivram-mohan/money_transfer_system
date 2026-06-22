package com.fidelity.moneytransfer.service;

import com.fidelity.moneytransfer.constants.RewardConstants;
import com.fidelity.moneytransfer.dto.RedeemResponse;
import com.fidelity.moneytransfer.dto.RewardProfileResponse;
import com.fidelity.moneytransfer.dto.RewardResult;
import com.fidelity.moneytransfer.dto.TransferRequest;
import com.fidelity.moneytransfer.entity.Account;
import com.fidelity.moneytransfer.entity.AppUser;
import com.fidelity.moneytransfer.entity.BankDetails;
import com.fidelity.moneytransfer.entity.RewardLedger;
import com.fidelity.moneytransfer.entity.TransactionLog;
import com.fidelity.moneytransfer.enums.AccountStatus;
import com.fidelity.moneytransfer.exception.AccountNotFoundException;
import com.fidelity.moneytransfer.repository.AccountRepository;
import com.fidelity.moneytransfer.repository.BankDetailsRepository;
import com.fidelity.moneytransfer.repository.RewardLedgerRepository;
import com.fidelity.moneytransfer.repository.TransactionLogRepository;
import com.fidelity.moneytransfer.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RewardServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private BankDetailsRepository bankDetailsRepository;
    @Mock private RewardLedgerRepository rewardLedgerRepository;
    @Mock private TransactionLogRepository transactionLogRepository;

    @InjectMocks private RewardService rewardService;

    private AppUser sender;
    private TransferRequest request;

    // A fixed weekday (Wednesday) and weekend (Saturday) for deterministic tests.
    private static final LocalDate WEDNESDAY = LocalDate.of(2024, 1, 3);
    private static final LocalDate SATURDAY = LocalDate.of(2024, 1, 6);

    @BeforeEach
    void setUp() {
        sender = AppUser.builder()
                .id(1L).username("john").name("John").email("john@example.com")
                .role("USER").status("ACTIVE").accountId(100L)
                .rewardPoints(0L).lifetimeRewardPoints(0L)
                .build();

        request = TransferRequest.builder()
                .fromAccountId(100L).toAccountId(200L)
                .amount(new BigDecimal("1000.00")).idempotencyKey("k1")
                .build();
    }

    // ─── processReward ────────────────────────────────────────────────

    @Test
    void processReward_amountNull_returnsNone() {
        request.setAmount(null);
        RewardResult r = rewardService.processReward(request, "tx");
        assertFalse(r.isRewarded());
        assertTrue(r.getMessage().contains("₹100"));
    }

    @Test
    void processReward_amountTooSmall_returnsNone() {
        request.setAmount(new BigDecimal("100.00")); // not > 100
        RewardResult r = rewardService.processReward(request, "tx");
        assertFalse(r.isRewarded());
    }

    @Test
    void processReward_fromIdNull_returnsNone() {
        request.setFromAccountId(null);
        RewardResult r = rewardService.processReward(request, "tx");
        assertFalse(r.isRewarded());
        assertNull(r.getMessage());
    }

    @Test
    void processReward_selfTransfer_returnsNone() {
        request.setToAccountId(100L); // equals fromId
        RewardResult r = rewardService.processReward(request, "tx");
        assertFalse(r.isRewarded());
    }

    @Test
    void processReward_senderNotRegistered_returnsNone() {
        when(userRepository.findByAccountId(100L)).thenReturn(Optional.empty());
        RewardResult r = rewardService.processReward(request, "tx");
        assertFalse(r.isRewarded());
    }

    @Test
    void processReward_alreadyRewardedToday_returnsNone() {
        when(userRepository.findByAccountId(100L)).thenReturn(Optional.of(sender));
        when(rewardLedgerRepository.existsByFromAccountIdAndToAccountIdAndRewardDate(
                eq(100L), eq(200L), any(LocalDate.class))).thenReturn(true);

        RewardResult r = rewardService.processReward(request, "tx");
        assertFalse(r.isRewarded());
        assertTrue(r.getMessage().contains("already earned"));
    }

    @Test
    void processReward_weekday_awardsBasePoints() {
        when(userRepository.findByAccountId(100L)).thenReturn(Optional.of(sender));
        when(rewardLedgerRepository.existsByFromAccountIdAndToAccountIdAndRewardDate(
                anyLong(), anyLong(), any())).thenReturn(false);

        try (MockedStatic<LocalDate> mocked = mockStatic(LocalDate.class, org.mockito.Answers.CALLS_REAL_METHODS)) {
            mocked.when(LocalDate::now).thenReturn(WEDNESDAY);

            RewardResult r = rewardService.processReward(request, "tx");

            assertTrue(r.isRewarded());
            assertEquals(10L, r.getPointsEarned()); // 1000/100 = 10
            assertFalse(r.isWeekendBonus());
            assertEquals(10L, r.getPointsBalance());
            assertEquals(10L, r.getLifetimePoints());
            assertFalse(r.isReachedRedeemThreshold());
            assertTrue(r.getMessage().contains("10 reward points"));
        }
        verify(userRepository).save(sender);
        verify(rewardLedgerRepository).save(any(RewardLedger.class));
    }

    @Test
    void processReward_weekend_doublesPointsAndCrossesThreshold() {
        sender.setRewardPoints(490L); // close to 500 threshold
        sender.setLifetimeRewardPoints(490L);
        request.setAmount(new BigDecimal("1000.00"));
        when(userRepository.findByAccountId(100L)).thenReturn(Optional.of(sender));
        when(rewardLedgerRepository.existsByFromAccountIdAndToAccountIdAndRewardDate(
                anyLong(), anyLong(), any())).thenReturn(false);

        try (MockedStatic<LocalDate> mocked = mockStatic(LocalDate.class, org.mockito.Answers.CALLS_REAL_METHODS)) {
            mocked.when(LocalDate::now).thenReturn(SATURDAY);

            RewardResult r = rewardService.processReward(request, "tx");

            assertTrue(r.isRewarded());
            assertEquals(20L, r.getPointsEarned()); // (1000/100)*2
            assertTrue(r.isWeekendBonus());
            assertEquals(510L, r.getPointsBalance());
            assertTrue(r.isReachedRedeemThreshold());
            assertTrue(r.getMessage().contains("Weekend bonus"));
        }
    }

    @Test
    void processReward_nullExistingPoints_treatedAsZero() {
        sender.setRewardPoints(null);
        sender.setLifetimeRewardPoints(null);
        when(userRepository.findByAccountId(100L)).thenReturn(Optional.of(sender));
        when(rewardLedgerRepository.existsByFromAccountIdAndToAccountIdAndRewardDate(
                anyLong(), anyLong(), any())).thenReturn(false);

        try (MockedStatic<LocalDate> mocked = mockStatic(LocalDate.class, org.mockito.Answers.CALLS_REAL_METHODS)) {
            mocked.when(LocalDate::now).thenReturn(WEDNESDAY);
            RewardResult r = rewardService.processReward(request, "tx");
            assertEquals(10L, r.getPointsBalance());
        }
    }

    @Test
    void processReward_sunday_isWeekend() {
        LocalDate sunday = LocalDate.of(2024, 1, 7);
        when(userRepository.findByAccountId(100L)).thenReturn(Optional.of(sender));
        when(rewardLedgerRepository.existsByFromAccountIdAndToAccountIdAndRewardDate(
                anyLong(), anyLong(), any())).thenReturn(false);

        try (MockedStatic<LocalDate> mocked = mockStatic(LocalDate.class, org.mockito.Answers.CALLS_REAL_METHODS)) {
            mocked.when(LocalDate::now).thenReturn(sunday);
            RewardResult r = rewardService.processReward(request, "tx");
            assertTrue(r.isWeekendBonus());
            assertEquals(20L, r.getPointsEarned());
        }
    }

    @Test
    void processReward_alreadyAboveThreshold_notReachedAgain() {
        sender.setRewardPoints(800L); // already past 500 threshold
        sender.setLifetimeRewardPoints(800L);
        when(userRepository.findByAccountId(100L)).thenReturn(Optional.of(sender));
        when(rewardLedgerRepository.existsByFromAccountIdAndToAccountIdAndRewardDate(
                anyLong(), anyLong(), any())).thenReturn(false);

        try (MockedStatic<LocalDate> mocked = mockStatic(LocalDate.class, org.mockito.Answers.CALLS_REAL_METHODS)) {
            mocked.when(LocalDate::now).thenReturn(WEDNESDAY);
            RewardResult r = rewardService.processReward(request, "tx");
            assertTrue(r.isRewarded());
            assertFalse(r.isReachedRedeemThreshold()); // oldBalance already >= threshold
        }
    }

    // ─── getProfile ───────────────────────────────────────────────────

    @Test
    void getProfile_belowThreshold_cannotRedeem() {
        sender.setRewardPoints(250L);
        sender.setLifetimeRewardPoints(250L);
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(sender));

        RewardProfileResponse p = rewardService.getProfile("john");
        assertEquals(250L, p.getPointsBalance());
        assertEquals(500L, p.getRedeemThreshold());
        assertFalse(p.isCanRedeem());
        assertEquals(50, p.getProgressPercent()); // 250*100/500
    }

    @Test
    void getProfile_aboveThreshold_capsProgressAt100() {
        sender.setRewardPoints(1200L);
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(sender));

        RewardProfileResponse p = rewardService.getProfile("john");
        assertTrue(p.isCanRedeem());
        assertEquals(100, p.getProgressPercent());
    }

    @Test
    void getProfile_userNotFound_throws() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        assertThrows(AccountNotFoundException.class, () -> rewardService.getProfile("ghost"));
    }

    // ─── redeem ───────────────────────────────────────────────────────

    @Test
    void redeem_success_withBankSync() {
        sender.setRewardPoints(600L);
        Account cashback = Account.builder().id(RewardConstants.CASHBACK_ACCOUNT_ID)
                .holderName("CASHBACK").balance(new BigDecimal("1000000.00"))
                .status(AccountStatus.ACTIVE).version(0).build();
        Account userAcct = Account.builder().id(100L).holderName("John")
                .balance(new BigDecimal("500.00")).status(AccountStatus.ACTIVE).version(0).build();
        BankDetails bank = BankDetails.builder().accountNumber(100L).userName("John")
                .email("john@example.com").balance(new BigDecimal("500.00")).registered(true).build();

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(sender));
        when(accountRepository.findById(RewardConstants.CASHBACK_ACCOUNT_ID)).thenReturn(Optional.of(cashback));
        when(accountRepository.findById(100L)).thenReturn(Optional.of(userAcct));
        when(bankDetailsRepository.findByAccountNumber(100L)).thenReturn(Optional.of(bank));
        when(transactionLogRepository.save(any(TransactionLog.class))).thenAnswer(inv -> {
            TransactionLog t = inv.getArgument(0); t.setId("txid"); return t;
        });

        RedeemResponse resp = rewardService.redeem("john", 500L);

        assertEquals(500L, resp.getRedeemedPoints());
        assertEquals(new BigDecimal("500.00"), resp.getAmountCredited());
        assertEquals(100L, resp.getRemainingPoints());
        assertEquals("txid", resp.getTransactionId());
        assertEquals(new BigDecimal("1000.00").movePointRight(0), userAcct.getBalance());
        assertEquals(100L, sender.getRewardPoints());
        verify(bankDetailsRepository).save(bank);
    }

    @Test
    void redeem_noBankDetails_skipsSync() {
        sender.setRewardPoints(600L);
        Account cashback = Account.builder().id(RewardConstants.CASHBACK_ACCOUNT_ID)
                .holderName("CASHBACK").balance(new BigDecimal("1000000.00"))
                .status(AccountStatus.ACTIVE).version(0).build();
        Account userAcct = Account.builder().id(100L).holderName("John")
                .balance(new BigDecimal("500.00")).status(AccountStatus.ACTIVE).version(0).build();

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(sender));
        when(accountRepository.findById(RewardConstants.CASHBACK_ACCOUNT_ID)).thenReturn(Optional.of(cashback));
        when(accountRepository.findById(100L)).thenReturn(Optional.of(userAcct));
        when(bankDetailsRepository.findByAccountNumber(100L)).thenReturn(Optional.empty());
        when(transactionLogRepository.save(any(TransactionLog.class))).thenAnswer(inv -> inv.getArgument(0));

        RedeemResponse resp = rewardService.redeem("john", 500L);
        assertEquals(500L, resp.getRedeemedPoints());
        verify(bankDetailsRepository, never()).save(any());
    }

    @Test
    void redeem_belowMinimum_throws() {
        sender.setRewardPoints(600L);
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(sender));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> rewardService.redeem("john", 100L));
        assertTrue(ex.getMessage().contains("Minimum redemption"));
    }

    @Test
    void redeem_morePointsThanBalance_throws() {
        sender.setRewardPoints(600L);
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(sender));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> rewardService.redeem("john", 900L));
        assertTrue(ex.getMessage().contains("only have 600"));
    }

    @Test
    void redeem_noLinkedAccount_throws() {
        sender.setRewardPoints(600L);
        sender.setAccountId(null);
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(sender));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> rewardService.redeem("john", 500L));
        assertTrue(ex.getMessage().contains("Link a bank account"));
    }

    @Test
    void redeem_cashbackAccountMissing_throws() {
        sender.setRewardPoints(600L);
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(sender));
        when(accountRepository.findById(RewardConstants.CASHBACK_ACCOUNT_ID)).thenReturn(Optional.empty());
        AccountNotFoundException ex = assertThrows(AccountNotFoundException.class,
                () -> rewardService.redeem("john", 500L));
        assertTrue(ex.getMessage().contains("Cashback account"));
    }

    @Test
    void redeem_userAccountMissing_throws() {
        sender.setRewardPoints(600L);
        Account cashback = Account.builder().id(RewardConstants.CASHBACK_ACCOUNT_ID)
                .balance(new BigDecimal("1000000.00")).status(AccountStatus.ACTIVE).version(0).build();
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(sender));
        when(accountRepository.findById(RewardConstants.CASHBACK_ACCOUNT_ID)).thenReturn(Optional.of(cashback));
        when(accountRepository.findById(100L)).thenReturn(Optional.empty());
        assertThrows(AccountNotFoundException.class, () -> rewardService.redeem("john", 500L));
    }
}
