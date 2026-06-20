package com.fidelity.moneytransfer.service;

import com.fidelity.moneytransfer.constants.RewardConstants;
import com.fidelity.moneytransfer.dto.RedeemResponse;
import com.fidelity.moneytransfer.dto.RewardProfileResponse;
import com.fidelity.moneytransfer.dto.RewardResult;
import com.fidelity.moneytransfer.dto.TransferRequest;
import com.fidelity.moneytransfer.entity.Account;
import com.fidelity.moneytransfer.entity.AppUser;
import com.fidelity.moneytransfer.entity.RewardLedger;
import com.fidelity.moneytransfer.entity.TransactionLog;
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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Owns the rewards domain: awarding points on qualifying transfers (with a
 * weekend 2x bonus), enforcing the once-per-day-per-directed-pair rule, exposing
 * the reward profile, and redeeming points as cash from the corporate CASHBACK
 * account.
 *
 * <p>{@link #processReward} is intentionally NOT {@code @Transactional} so it
 * joins the caller's (transfer) transaction without opening a new boundary. That
 * lets {@code TransferService} catch any reward failure WITHOUT marking the
 * transfer rollback-only — a broken reward must never fail a valid transfer.
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
        // Rules 3 & 4: different accounts / not a self-transfer.
        if (fromId == null || fromId.equals(toId)) {
            return RewardResult.none(null);
        }

        // Points accrue to the registered sender; skip system/admin accounts.
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

        boolean weekend = isWeekend(today);
        long basePoints = amount.divideToIntegralValue(RewardConstants.POINTS_EARN_DIVISOR)
                .longValueExact();
        long pointsEarned = weekend ? basePoints * RewardConstants.WEEKEND_MULTIPLIER : basePoints;

        long oldBalance = nz(sender.getRewardPoints());
        long newBalance = oldBalance + pointsEarned;
        long newLifetime = nz(sender.getLifetimeRewardPoints()) + pointsEarned;

        sender.setRewardPoints(newBalance);
        sender.setLifetimeRewardPoints(newLifetime);
        userRepository.save(sender);

        rewardLedgerRepository.save(RewardLedger.builder()
                .userId(sender.getId())
                .fromAccountId(fromId)
                .toAccountId(toId)
                .transactionId(transactionId)
                .pointsEarned(pointsEarned)
                .weekendBonus(weekend)
                .rewardDate(today)
                .build());

        boolean reachedThreshold = oldBalance < RewardConstants.REDEEM_THRESHOLD
                && newBalance >= RewardConstants.REDEEM_THRESHOLD;

        log.info("Rewarded user {} (+{} pts{}). Balance {} -> {}",
                sender.getId(), pointsEarned, weekend ? ", weekend 2x" : "", oldBalance, newBalance);

        return RewardResult.builder()
                .rewarded(true)
                .pointsEarned(pointsEarned)
                .weekendBonus(weekend)
                .pointsBalance(newBalance)
                .lifetimePoints(newLifetime)
                .reachedRedeemThreshold(reachedThreshold)
                .message(weekend
                        ? "Weekend bonus! You earned " + pointsEarned + " reward points (2x)."
                        : "You earned " + pointsEarned + " reward points.")
                .build();
    }

    // ─── PROFILE ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public RewardProfileResponse getProfile(String username) {
        AppUser user = requireUser(username);
        long balance = nz(user.getRewardPoints());
        long threshold = RewardConstants.REDEEM_THRESHOLD;

        return RewardProfileResponse.builder()
                .holderName(user.getName())
                .pointsBalance(balance)
                .lifetimePoints(nz(user.getLifetimeRewardPoints()))
                .redeemThreshold(threshold)
                .canRedeem(balance >= threshold)
                .progressPercent((int) Math.max(0, Math.min(100, balance * 100 / threshold)))
                .weekendBonusActive(isWeekend(LocalDate.now()))
                .build();
    }

    // ─── REDEMPTION (points -> cash from the corporate account) ──────────

    @Transactional
    public RedeemResponse redeem(String username, long points) {
        AppUser user = requireUser(username);

        if (points < RewardConstants.REDEEM_THRESHOLD) {
            throw new IllegalArgumentException(
                    "Minimum redemption is " + RewardConstants.REDEEM_THRESHOLD + " points.");
        }
        long balance = nz(user.getRewardPoints());
        if (points > balance) {
            throw new IllegalArgumentException(
                    "You only have " + balance + " reward points.");
        }
        if (user.getAccountId() == null) {
            throw new IllegalArgumentException(
                    "Link a bank account before redeeming reward points.");
        }

        Account cashbackAccount = accountRepository.findById(RewardConstants.CASHBACK_ACCOUNT_ID)
                .orElseThrow(() -> new AccountNotFoundException("Cashback account is unavailable"));
        Account userAccount = accountRepository.findById(user.getAccountId())
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found: " + user.getAccountId()));

        BigDecimal amount = BigDecimal.valueOf(points).setScale(2);

        cashbackAccount.debit(amount);   // throws if the float is somehow exhausted
        userAccount.credit(amount);
        accountRepository.save(cashbackAccount);
        accountRepository.save(userAccount);
        syncBankDetailsBalance(userAccount);

        TransactionLog cashbackTxn = transactionLogRepository.save(TransactionLog.builder()
                .fromAccountId(RewardConstants.CASHBACK_ACCOUNT_ID)
                .toAccountId(userAccount.getId())
                .amount(amount)
                .status(TransactionStatus.SUCCESS)
                .idempotencyKey("redeem-" + user.getId() + "-" + System.currentTimeMillis())
                .build());

        long remaining = balance - points;
        user.setRewardPoints(remaining);
        userRepository.save(user);

        log.info("User {} redeemed {} points for ₹{} (remaining {})",
                user.getId(), points, amount, remaining);

        return RedeemResponse.builder()
                .redeemedPoints(points)
                .amountCredited(amount)
                .remainingPoints(remaining)
                .transactionId(cashbackTxn.getId())
                .message("₹" + amount.toPlainString() + " credited to your account.")
                .build();
    }

    // ─── helpers ─────────────────────────────────────────────────────────

    private void syncBankDetailsBalance(Account account) {
        bankDetailsRepository.findByAccountNumber(account.getId())
                .ifPresent(bankDetails -> {
                    bankDetails.setBalance(account.getBalance());
                    bankDetailsRepository.save(bankDetails);
                });
    }

    private AppUser requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AccountNotFoundException(
                        "No user found with username: " + username));
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    private long nz(Long value) {
        return value != null ? value : 0L;
    }
}
