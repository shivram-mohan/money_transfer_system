package com.fidelity.moneytransfer.service;

import com.fidelity.moneytransfer.constants.RewardConstants;
import com.fidelity.moneytransfer.dto.MonthlySummaryResponse;
import com.fidelity.moneytransfer.dto.RewardProfileResponse;
import com.fidelity.moneytransfer.dto.RewardResult;
import com.fidelity.moneytransfer.dto.TransferRequest;
import com.fidelity.moneytransfer.entity.Account;
import com.fidelity.moneytransfer.entity.AppUser;
import com.fidelity.moneytransfer.entity.BankDetails;
import com.fidelity.moneytransfer.entity.RewardLedger;
import com.fidelity.moneytransfer.enums.AccountStatus;
import com.fidelity.moneytransfer.enums.Tier;
import com.fidelity.moneytransfer.entity.TransactionLog;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RewardServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private BankDetailsRepository bankDetailsRepository;
    @Mock private RewardLedgerRepository rewardLedgerRepository;
    @Mock private TransactionLogRepository transactionLogRepository;

    @InjectMocks private RewardService rewardService;

    private AppUser sender;
    private Account cashbackAccount;
    private Account senderAccount;

    @BeforeEach
    void setUp() {
        sender = AppUser.builder()
                .id(1L).username("john").name("John").email("john@example.com")
                .role("USER").status("ACTIVE").accountId(1001L)
                .rewardPoints(0L).tier("BRONZE").downgradeWarningSent(false)
                .build();
        cashbackAccount = Account.builder()
                .id(RewardConstants.CASHBACK_ACCOUNT_ID).holderName("CASHBACK")
                .balance(new BigDecimal("1000000.00"))
                .status(AccountStatus.ACTIVE).version(0).build();
        senderAccount = Account.builder()
                .id(1001L).holderName("John").balance(new BigDecimal("100.00"))
                .status(AccountStatus.ACTIVE).version(0).build();

        when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));
        when(transactionLogRepository.save(any(TransactionLog.class)))
                .thenAnswer(i -> i.getArgument(0));
    }

    private TransferRequest req(String amount, Long from, Long to) {
        return TransferRequest.builder()
                .fromAccountId(from).toAccountId(to)
                .amount(amount == null ? null : new BigDecimal(amount))
                .idempotencyKey("k").build();
    }

    private void stubCashbackPayable() {
        when(accountRepository.findById(RewardConstants.CASHBACK_ACCOUNT_ID))
                .thenReturn(Optional.of(cashbackAccount));
        when(accountRepository.findById(1001L)).thenReturn(Optional.of(senderAccount));
        when(bankDetailsRepository.findByAccountNumber(1001L))
                .thenReturn(Optional.of(BankDetails.builder()
                        .accountNumber(1001L).userName("John").email("john@example.com")
                        .balance(new BigDecimal("100.00")).registered(true).build()));
    }

    // ─── EARNING: no-reward branches ─────────────────────────────────

    @Test
    void processReward_AmountTooSmall_NoReward() {
        RewardResult r = rewardService.processReward(req("100.00", 1001L, 1002L), "t");
        assertFalse(r.isRewarded());
        assertEquals(BigDecimal.ZERO, r.getCashbackAmount());
    }

    @Test
    void processReward_NullAmount_NoReward() {
        RewardResult r = rewardService.processReward(req(null, 1001L, 1002L), "t");
        assertFalse(r.isRewarded());
    }

    @Test
    void processReward_SelfTransfer_NoReward() {
        assertFalse(rewardService.processReward(req("500.00", 1001L, 1001L), "t").isRewarded());
    }

    @Test
    void processReward_SenderNotRegistered_NoReward() {
        when(userRepository.findByAccountId(1001L)).thenReturn(Optional.empty());
        assertFalse(rewardService.processReward(req("500.00", 1001L, 1002L), "t").isRewarded());
    }

    @Test
    void processReward_AlreadyRewardedToday_NoReward() {
        when(userRepository.findByAccountId(1001L)).thenReturn(Optional.of(sender));
        when(rewardLedgerRepository
                .existsByFromAccountIdAndToAccountIdAndRewardDate(eq(1001L), eq(1002L), any()))
                .thenReturn(true);
        assertFalse(rewardService.processReward(req("500.00", 1001L, 1002L), "t").isRewarded());
        verify(userRepository, never()).save(any());
    }

    // ─── EARNING: success / cashback / tiers ─────────────────────────

    @Test
    void processReward_BronzeUser_EarnsBasePoints_AndCashback() {
        when(userRepository.findByAccountId(1001L)).thenReturn(Optional.of(sender));
        when(rewardLedgerRepository
                .existsByFromAccountIdAndToAccountIdAndRewardDate(any(), any(), any()))
                .thenReturn(false);
        stubCashbackPayable();

        RewardResult r = rewardService.processReward(req("1000.00", 1001L, 1002L), "t");

        assertTrue(r.isRewarded());
        assertEquals(10L, r.getPointsEarned()); // 1000/100 * 1.0
        assertEquals(10L, r.getTotalPoints());
        assertEquals("BRONZE", r.getTier());
        assertFalse(r.isTierUpgraded());
        // cashback is a guaranteed integer in [1, pointsEarned]
        assertTrue(r.getCashbackAmount().compareTo(BigDecimal.ONE) >= 0);
        assertTrue(r.getCashbackAmount().compareTo(new BigDecimal("10")) <= 0);
        assertEquals(LocalDateTime.class, sender.getLastTransactionDate().getClass());
        verify(rewardLedgerRepository).save(any(RewardLedger.class));
    }

    @Test
    void processReward_CrossesIntoSilver_FlagsUpgrade() {
        sender.setRewardPoints(499L);
        when(userRepository.findByAccountId(1001L)).thenReturn(Optional.of(sender));
        when(rewardLedgerRepository
                .existsByFromAccountIdAndToAccountIdAndRewardDate(any(), any(), any()))
                .thenReturn(false);
        stubCashbackPayable();

        RewardResult r = rewardService.processReward(req("1000.00", 1001L, 1002L), "t");

        assertTrue(r.isRewarded());
        assertEquals("SILVER", r.getTier());
        assertTrue(r.isTierUpgraded());
        assertEquals("GOLD", r.getNextTier());
        assertTrue(r.getMessage().contains("SILVER"));
    }

    @Test
    void processReward_SilverMultiplierFloored() {
        sender.setRewardPoints(500L); // SILVER, 1.5x
        when(userRepository.findByAccountId(1001L)).thenReturn(Optional.of(sender));
        when(rewardLedgerRepository
                .existsByFromAccountIdAndToAccountIdAndRewardDate(any(), any(), any()))
                .thenReturn(false);
        stubCashbackPayable();

        // base 10 * 1.5 = 15
        RewardResult r = rewardService.processReward(req("1000.00", 1001L, 1002L), "t");
        assertEquals(15L, r.getPointsEarned());
    }

    @Test
    void processReward_CashbackAccountMissing_StillAwardsPoints_NoCashback() {
        when(userRepository.findByAccountId(1001L)).thenReturn(Optional.of(sender));
        when(rewardLedgerRepository
                .existsByFromAccountIdAndToAccountIdAndRewardDate(any(), any(), any()))
                .thenReturn(false);
        when(accountRepository.findById(RewardConstants.CASHBACK_ACCOUNT_ID))
                .thenReturn(Optional.empty());

        RewardResult r = rewardService.processReward(req("1000.00", 1001L, 1002L), "t");

        assertTrue(r.isRewarded());
        assertEquals(BigDecimal.ZERO, r.getCashbackAmount());
    }

    @Test
    void processReward_ReceiverAccountMissing_NoCashback() {
        when(userRepository.findByAccountId(1001L)).thenReturn(Optional.of(sender));
        when(rewardLedgerRepository
                .existsByFromAccountIdAndToAccountIdAndRewardDate(any(), any(), any()))
                .thenReturn(false);
        // cashback account present, but the receiver account lookup is empty
        when(accountRepository.findById(RewardConstants.CASHBACK_ACCOUNT_ID))
                .thenReturn(Optional.of(cashbackAccount));
        when(accountRepository.findById(1001L)).thenReturn(Optional.empty());

        RewardResult r = rewardService.processReward(req("1000.00", 1001L, 1002L), "t");

        assertTrue(r.isRewarded());
        assertEquals(BigDecimal.ZERO, r.getCashbackAmount());
    }

    @Test
    void processReward_CashbackAccountExhausted_NoCashback() {
        cashbackAccount.setBalance(new BigDecimal("0.00"));
        when(userRepository.findByAccountId(1001L)).thenReturn(Optional.of(sender));
        when(rewardLedgerRepository
                .existsByFromAccountIdAndToAccountIdAndRewardDate(any(), any(), any()))
                .thenReturn(false);
        when(accountRepository.findById(RewardConstants.CASHBACK_ACCOUNT_ID))
                .thenReturn(Optional.of(cashbackAccount));
        when(accountRepository.findById(1001L)).thenReturn(Optional.of(senderAccount));

        RewardResult r = rewardService.processReward(req("1000.00", 1001L, 1002L), "t");

        assertTrue(r.isRewarded());
        assertEquals(BigDecimal.ZERO, r.getCashbackAmount());
    }

    // ─── PROFILE ─────────────────────────────────────────────────────

    @Test
    void getProfile_BuildsTierAndProgress() {
        sender.setRewardPoints(250L);
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(sender));
        when(rewardLedgerRepository.sumCashback(eq(1L), any(), any()))
                .thenReturn(new BigDecimal("42.00"));

        RewardProfileResponse p = rewardService.getProfile("john");

        assertEquals("John", p.getHolderName());
        assertEquals(250L, p.getTotalPoints());
        assertEquals("BRONZE", p.getTier());
        assertEquals("SILVER", p.getNextTier());
        assertEquals(250L, p.getPointsToNextTier()); // 500-250
        assertEquals(50, p.getProgressPercent());    // 250/500
        assertEquals(new BigDecimal("42.00"), p.getTotalCashbackEarned());
    }

    @Test
    void getProfile_Platinum_Progress100_NullCashbackHandled() {
        sender.setRewardPoints(6000L);
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(sender));
        when(rewardLedgerRepository.sumCashback(anyLong(), any(), any())).thenReturn(null);

        RewardProfileResponse p = rewardService.getProfile("john");

        assertEquals("PLATINUM", p.getTier());
        assertNull(p.getNextTier());
        assertEquals(100, p.getProgressPercent());
        assertEquals(0L, p.getPointsToNextTier());
        assertEquals(BigDecimal.ZERO, p.getTotalCashbackEarned());
    }

    @Test
    void getProfile_UserNotFound_Throws() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        assertThrows(AccountNotFoundException.class, () -> rewardService.getProfile("ghost"));
    }

    // ─── MONTHLY SUMMARY ─────────────────────────────────────────────

    @Test
    void getMonthlySummary_AggregatesLedger() {
        sender.setRewardPoints(2000L);
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(sender));
        when(rewardLedgerRepository.sumPointsEarned(eq(1L), any(), any())).thenReturn(120L);
        when(rewardLedgerRepository.sumCashback(eq(1L), any(), any()))
                .thenReturn(new BigDecimal("55.00"));
        when(rewardLedgerRepository.countByUserIdAndCreatedOnBetween(eq(1L), any(), any()))
                .thenReturn(7L);

        MonthlySummaryResponse s = rewardService.getMonthlySummary("john", YearMonth.of(2026, 6));

        assertEquals("June 2026", s.getMonth());
        assertEquals(7L, s.getRewardedTransfers());
        assertEquals(120L, s.getPointsEarned());
        assertEquals(new BigDecimal("55.00"), s.getCashbackEarned());
        assertEquals("GOLD", s.getTier());
        assertEquals(2000L, s.getTotalPoints());
    }

    @Test
    void getMonthlySummary_NullCashback_DefaultsZero() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(sender));
        when(rewardLedgerRepository.sumPointsEarned(anyLong(), any(), any())).thenReturn(0L);
        when(rewardLedgerRepository.sumCashback(anyLong(), any(), any())).thenReturn(null);
        when(rewardLedgerRepository.countByUserIdAndCreatedOnBetween(anyLong(), any(), any()))
                .thenReturn(0L);

        MonthlySummaryResponse s = rewardService.getMonthlySummary("john", YearMonth.of(2026, 1));
        assertEquals(BigDecimal.ZERO, s.getCashbackEarned());
        assertEquals("January 2026", s.getMonth());
    }

    @Test
    void getMonthlySummary_UserNotFound_Throws() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        assertThrows(AccountNotFoundException.class,
                () -> rewardService.getMonthlySummary("ghost", YearMonth.now()));
    }

    @Test
    void processReward_CloseToNextTier_FlaggedTrue() {
        sender.setRewardPoints(485L); // BRONZE, 15 below SILVER (500)
        when(userRepository.findByAccountId(1001L)).thenReturn(Optional.of(sender));
        when(rewardLedgerRepository
                .existsByFromAccountIdAndToAccountIdAndRewardDate(any(), any(), any()))
                .thenReturn(false);
        stubCashbackPayable();

        // +10 -> 495, still BRONZE, only 5 to SILVER -> close
        RewardResult r = rewardService.processReward(req("1000.00", 1001L, 1002L), "t");

        assertEquals("BRONZE", r.getTier());
        assertFalse(r.isTierUpgraded());
        assertTrue(r.isCloseToNextTier());
    }

    @Test
    void processReward_PlatinumEarner_NoNextTier() {
        sender.setRewardPoints(5000L); // PLATINUM
        when(userRepository.findByAccountId(1001L)).thenReturn(Optional.of(sender));
        when(rewardLedgerRepository
                .existsByFromAccountIdAndToAccountIdAndRewardDate(any(), any(), any()))
                .thenReturn(false);
        stubCashbackPayable();

        RewardResult r = rewardService.processReward(req("1000.00", 1001L, 1002L), "t");

        assertEquals("PLATINUM", r.getTier());
        assertNull(r.getNextTier());
        assertFalse(r.isCloseToNextTier());
        assertEquals(0L, r.getPointsToNextTier());
    }

    @Test
    void getProfile_NullRewardPoints_TreatedAsZero() {
        sender.setRewardPoints(null);
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(sender));
        when(rewardLedgerRepository.sumCashback(anyLong(), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        RewardProfileResponse p = rewardService.getProfile("john");

        assertEquals(0L, p.getTotalPoints());
        assertEquals("BRONZE", p.getTier());
    }
}
