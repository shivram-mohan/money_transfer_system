package com.fidelity.moneytransfer.service;

import com.fidelity.moneytransfer.constants.RewardConstants;
import com.fidelity.moneytransfer.dto.MonthlySummaryResponse;
import com.fidelity.moneytransfer.dto.RewardProfileResponse;
import com.fidelity.moneytransfer.dto.RewardResult;
import com.fidelity.moneytransfer.dto.TransferRequest;
import com.fidelity.moneytransfer.entity.Account;
import com.fidelity.moneytransfer.entity.AppUser;
import com.fidelity.moneytransfer.entity.RewardLedger;
import com.fidelity.moneytransfer.entity.TransactionLog;
import com.fidelity.moneytransfer.enums.Tier;
import com.fidelity.moneytransfer.enums.TransactionStatus;
import com.fidelity.moneytransfer.exception.AccountNotFoundException;
import com.fidelity.moneytransfer.repository.AccountRepository;
import com.fidelity.moneytransfer.repository.BankDetailsRepository;
import com.fidelity.moneytransfer.repository.RewardLedgerRepository;
import com.fidelity.moneytransfer.repository.TransactionLogRepository;
import com.fidelity.moneytransfer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Owns the rewards domain: awarding points (with the tier multiplier), paying
 * guaranteed cashback from the corporate CASHBACK account, enforcing the
 * once-per-day-per-directed-pair rule, and reading reward profiles / summaries.
 *
 * <p>{@link #processReward} is deliberately NOT annotated {@code @Transactional}
 * so it joins the caller's (transfer) transaction without opening a new
 * boundary. That lets {@code TransferService} catch any reward failure WITHOUT
 * poisoning the transfer with a rollback-only mark — a broken reward must never
 * fail an otherwise valid transfer.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RewardService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final BankDetailsRepository bankDetailsRepository;
    private final RewardLedgerRepository rewardLedgerRepository;
    private final TransactionLogRepository transactionLogRepository;

    // ─── EARNING (called from within the transfer transaction) ───────────

    public RewardResult processReward(TransferRequest request, String transactionId) {
        BigDecimal amount = request.getAmount();
        Long fromId = request.getFromAccountId();
        Long toId = request.getToAccountId();

        // Rule 2: amount must be greater than 100.
        if (amount == null || amount.compareTo(RewardConstants.MIN_REWARDABLE_AMOUNT) <= 0) {
            return RewardResult.none("Transfer over ₹100 to start earning reward points.");
        }
        // Rules 3 & 4: different users / not a self-transfer.
        if (fromId == null || fromId.equals(toId)) {
            return RewardResult.none(null);
        }

        // Rewards accrue to the registered sender; skip system/admin accounts.
        Optional<AppUser> senderOpt = userRepository.findByAccountId(fromId);
        if (senderOpt.isEmpty()) {
            return RewardResult.none(null);
        }
        AppUser sender = senderOpt.get();

        // Only the FIRST transfer per day for this directed pair earns rewards.
        LocalDate today = LocalDate.now();
        if (rewardLedgerRepository
                .existsByFromAccountIdAndToAccountIdAndRewardDate(fromId, toId, today)) {
            return RewardResult.none(
                    "You've already earned today's reward for this recipient. "
                            + "Try again tomorrow!");
        }

        long currentPoints = nz(sender.getRewardPoints());
        Tier oldTier = Tier.fromPoints(currentPoints);

        long basePoints = amount.divideToIntegralValue(RewardConstants.POINTS_EARN_DIVISOR)
                .longValueExact();
        long pointsEarned = applyMultiplier(basePoints, oldTier);

        long newPoints = currentPoints + pointsEarned;
        Tier newTier = Tier.fromPoints(newPoints);
        boolean upgraded = newTier.ordinal() > oldTier.ordinal();

        // Guaranteed cashback in [1, pointsEarned], floored (integer points).
        BigDecimal cashback = payCashback(fromId, pointsEarned, transactionId);

        // Persist the user's new standing.
        sender.setRewardPoints(newPoints);
        sender.setTier(newTier.name());
        sender.setLastTransactionDate(LocalDateTime.now());
        sender.setDowngradeWarningSent(false);
        userRepository.save(sender);

        rewardLedgerRepository.save(RewardLedger.builder()
                .userId(sender.getId())
                .fromAccountId(fromId)
                .toAccountId(toId)
                .transactionId(transactionId)
                .pointsEarned(pointsEarned)
                .cashbackAmount(cashback)
                .rewardDate(today)
                .build());

        long pointsToNext = newTier.pointsToNext(newPoints);
        boolean closeToNext = newTier.next() != null
                && pointsToNext > 0
                && pointsToNext <= RewardConstants.CLOSE_TO_NEXT_TIER_THRESHOLD;

        log.info("Rewarded user {} (+{} pts, ₹{} cashback). Tier {} -> {}",
                sender.getId(), pointsEarned, cashback, oldTier, newTier);

        return RewardResult.builder()
                .rewarded(true)
                .pointsEarned(pointsEarned)
                .cashbackAmount(cashback)
                .totalPoints(newPoints)
                .tier(newTier.name())
                .nextTier(newTier.next() != null ? newTier.next().name() : null)
                .pointsToNextTier(pointsToNext)
                .tierUpgraded(upgraded)
                .closeToNextTier(closeToNext)
                .message(upgraded
                        ? "Congratulations! You've reached " + newTier.name() + " tier!"
                        : "You earned " + pointsEarned + " reward points.")
                .build();
    }

    private long applyMultiplier(long basePoints, Tier tier) {
        return BigDecimal.valueOf(basePoints)
                .multiply(tier.getMultiplier())
                .setScale(0, RoundingMode.FLOOR)
                .longValueExact();
    }

    /**
     * Moves a guaranteed cashback (1..pointsEarned) from the corporate CASHBACK
     * account to the sender and records it as a SUCCESS transaction. Returns the
     * amount paid (zero if the cashback account is unavailable, so points are
     * still awarded).
     */
    private BigDecimal payCashback(Long toAccountId, long pointsEarned, String sourceTransactionId) {
        if (pointsEarned <= 0) {
            return BigDecimal.ZERO;
        }
        Optional<Account> cashbackOpt =
                accountRepository.findById(RewardConstants.CASHBACK_ACCOUNT_ID);
        Optional<Account> receiverOpt = accountRepository.findById(toAccountId);
        if (cashbackOpt.isEmpty() || receiverOpt.isEmpty()) {
            log.warn("Cashback skipped: CASHBACK account or receiver {} missing", toAccountId);
            return BigDecimal.ZERO;
        }

        long cashbackUnits = ThreadLocalRandom.current().nextLong(1, pointsEarned + 1);
        BigDecimal cashback = BigDecimal.valueOf(cashbackUnits).setScale(2, RoundingMode.FLOOR);

        Account cashbackAccount = cashbackOpt.get();
        Account receiver = receiverOpt.get();
        if (cashbackAccount.getBalance().compareTo(cashback) < 0) {
            log.warn("CASHBACK account exhausted; skipping cashback payout");
            return BigDecimal.ZERO;
        }

        cashbackAccount.debit(cashback);
        receiver.credit(cashback);
        accountRepository.save(cashbackAccount);
        accountRepository.save(receiver);
        syncBankDetailsBalance(receiver);

        transactionLogRepository.save(TransactionLog.builder()
                .fromAccountId(RewardConstants.CASHBACK_ACCOUNT_ID)
                .toAccountId(toAccountId)
                .amount(cashback)
                .status(TransactionStatus.SUCCESS)
                .idempotencyKey("cashback-" + sourceTransactionId)
                .build());

        return cashback;
    }

    private void syncBankDetailsBalance(Account account) {
        bankDetailsRepository.findByAccountNumber(account.getId())
                .ifPresent(bankDetails -> {
                    bankDetails.setBalance(account.getBalance());
                    bankDetailsRepository.save(bankDetails);
                });
    }

    // ─── READ: profile & summaries ───────────────────────────────────────

    @Transactional(readOnly = true)
    public RewardProfileResponse getProfile(String username) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AccountNotFoundException(
                        "No user found with username: " + username));
        return buildProfile(user);
    }

    public RewardProfileResponse buildProfile(AppUser user) {
        long points = nz(user.getRewardPoints());
        Tier tier = Tier.fromPoints(points);
        Tier next = tier.next();

        BigDecimal lifetimeCashback = rewardLedgerRepository.sumCashback(
                user.getId(), LocalDateTime.MIN, LocalDateTime.now());

        return RewardProfileResponse.builder()
                .holderName(user.getName())
                .totalPoints(points)
                .tier(tier.name())
                .multiplier(tier.getMultiplier())
                .nextTier(next != null ? next.name() : null)
                .pointsToNextTier(tier.pointsToNext(points))
                .progressPercent(progressPercent(points, tier))
                .lastTransactionDate(user.getLastTransactionDate())
                .totalCashbackEarned(lifetimeCashback != null ? lifetimeCashback : BigDecimal.ZERO)
                .build();
    }

    @Transactional(readOnly = true)
    public MonthlySummaryResponse getMonthlySummary(String username, YearMonth month) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AccountNotFoundException(
                        "No user found with username: " + username));
        return buildMonthlySummary(user, month);
    }

    public MonthlySummaryResponse buildMonthlySummary(AppUser user, YearMonth month) {
        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.atEndOfMonth().atTime(23, 59, 59);

        long points = rewardLedgerRepository.sumPointsEarned(user.getId(), start, end);
        BigDecimal cashback = rewardLedgerRepository.sumCashback(user.getId(), start, end);
        long count = rewardLedgerRepository.countByUserIdAndCreatedOnBetween(user.getId(), start, end);

        long totalPoints = nz(user.getRewardPoints());
        return MonthlySummaryResponse.builder()
                .month(month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                        + " " + month.getYear())
                .rewardedTransfers(count)
                .pointsEarned(points)
                .cashbackEarned(cashback != null ? cashback : BigDecimal.ZERO)
                .tier(Tier.fromPoints(totalPoints).name())
                .totalPoints(totalPoints)
                .build();
    }

    private int progressPercent(long points, Tier tier) {
        Tier next = tier.next();
        if (next == null) {
            return 100;
        }
        long span = next.getMinPoints() - tier.getMinPoints();
        long progressed = points - tier.getMinPoints();
        if (span <= 0) {
            return 100;
        }
        return (int) Math.max(0, Math.min(100, Math.round(progressed * 100.0 / span)));
    }

    private long nz(Long value) {
        return value != null ? value : 0L;
    }
}
