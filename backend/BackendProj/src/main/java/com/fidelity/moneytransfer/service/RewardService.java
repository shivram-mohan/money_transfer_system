package com.fidelity.moneytransfer.service;

import com.fidelity.moneytransfer.dto.RewardEarnResult;
import com.fidelity.moneytransfer.dto.RewardResponse;
import com.fidelity.moneytransfer.entity.AppUser;
import com.fidelity.moneytransfer.entity.RewardAccount;
import com.fidelity.moneytransfer.enums.Tier;
import com.fidelity.moneytransfer.repository.RewardAccountRepository;
import com.fidelity.moneytransfer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Owns all rewards-programme logic: earning points on a transfer, deriving
 * tiers, nudging users towards the next tier, and the time-based jobs
 * (inactivity downgrades and monthly summaries). Points are the single source
 * of truth — a tier is always {@link Tier#fromPoints(long)} of the balance.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RewardService {

    /** Minimum transfer amount that earns any points. */
    private static final BigDecimal MIN_REWARDABLE_AMOUNT = new BigDecimal("100");

    /** Rupees that earn one base point. */
    private static final BigDecimal RUPEES_PER_POINT = new BigDecimal("100");

    /** A user this close (or closer) to the next tier gets an "almost there" nudge. */
    private static final long NEAR_TIER_THRESHOLD = 100;

    /** Days without earning before the tier is downgraded. */
    private static final long INACTIVITY_DOWNGRADE_DAYS = 30;

    /** Days of warning given before the downgrade actually happens. */
    private static final long DOWNGRADE_WARNING_LEAD_DAYS = 10;

    private final RewardAccountRepository rewardAccountRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    // ─── EARNING ─────────────────────────────────────────────────────

    /**
     * Awards reward points to the sender for a successful transfer. Runs inside
     * the caller's transaction so the points commit atomically with the money
     * movement. Returns a result describing what happened (points earned, tier
     * upgrade, near-tier nudge) for the API/UI, and fires the post-transaction
     * and upgrade alert emails.
     *
     * @return the earn result, or a non-earning result if the basic rules
     *         (amount &gt; 100, distinct accounts) are not met.
     */
    @Transactional
    public RewardEarnResult awardForTransfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        // Basic rules: distinct accounts and amount strictly greater than 100.
        // (The caller only invokes this on a SUCCESS transfer.)
        if (fromAccountId == null || toAccountId == null
                || fromAccountId.equals(toAccountId)
                || amount == null
                || amount.compareTo(MIN_REWARDABLE_AMOUNT) <= 0) {
            return RewardEarnResult.builder().earned(false).build();
        }

        RewardAccount reward = getOrCreate(fromAccountId);

        Tier earningTier = Tier.fromPoints(reward.getPoints());
        long basePoints = amount.divideToIntegralValue(RUPEES_PER_POINT).longValueExact();
        long earned = BigDecimal.valueOf(basePoints)
                .multiply(earningTier.getMultiplier())
                .setScale(0, RoundingMode.DOWN)
                .longValueExact();

        if (earned <= 0) {
            return RewardEarnResult.builder().earned(false).build();
        }

        Tier previousTier = reward.addPoints(earned);
        rewardAccountRepository.save(reward);

        boolean upgraded = reward.getTier().ordinal() > previousTier.ordinal();
        RewardResponse snapshot = toResponse(reward);
        boolean nearNext = isNearNextTier(reward);

        log.info("Awarded {} reward points to account {} ({} -> {}), new balance {}",
                earned, fromAccountId, previousTier, reward.getTier(), reward.getPoints());

        // Alerts (async, best-effort — never block or fail the transfer).
        AppUser user = userRepository.findByAccountId(fromAccountId).orElse(null);
        if (user != null && user.getEmail() != null) {
            emailService.sendRewardEarnedEmail(
                    user.getEmail(), user.getName(), earned, snapshot, upgraded);
        }

        return RewardEarnResult.builder()
                .earned(true)
                .pointsEarned(earned)
                .basePoints(basePoints)
                .tierUpgraded(upgraded)
                .nearNextTier(nearNext)
                .rewards(snapshot)
                .build();
    }

    // ─── QUERY ───────────────────────────────────────────────────────

    @Transactional
    public RewardResponse getRewards(Long accountId) {
        return toResponse(getOrCreate(accountId));
    }

    @Transactional
    public RewardAccount getOrCreate(Long accountId) {
        return rewardAccountRepository.findByAccountId(accountId)
                .orElseGet(() -> {
                    AppUser user = userRepository.findByAccountId(accountId).orElse(null);
                    RewardAccount created = RewardAccount.builder()
                            .accountId(accountId)
                            .userId(user != null ? user.getId() : null)
                            .points(0L)
                            .lifetimePoints(0L)
                            .tier(Tier.BRONZE)
                            .build();
                    return rewardAccountRepository.save(created);
                });
    }

    // ─── TIME-BASED JOBS ─────────────────────────────────────────────

    /**
     * Warns users who are approaching the inactivity downgrade and downgrades
     * those who have crossed it. Idempotent across runs: a downgraded account's
     * clock is reset so it is not downgraded again until it is inactive afresh.
     */
    @Transactional
    public void processInactivity() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime warnCutoff = now.minusDays(INACTIVITY_DOWNGRADE_DAYS - DOWNGRADE_WARNING_LEAD_DAYS);

        var staleAccounts = rewardAccountRepository
                .findByLastEarnedDateIsNotNullAndLastEarnedDateBefore(warnCutoff);
        log.info("Inactivity sweep: {} reward account(s) past the warning cut-off", staleAccounts.size());

        for (RewardAccount reward : staleAccounts) {
            long daysInactive = ChronoUnit.DAYS.between(reward.getLastEarnedDate(), now);
            AppUser user = reward.getUserId() != null
                    ? userRepository.findById(reward.getUserId()).orElse(null)
                    : userRepository.findByAccountId(reward.getAccountId()).orElse(null);

            if (daysInactive >= INACTIVITY_DOWNGRADE_DAYS) {
                Tier previous = reward.getTier();
                if (!previous.equals(Tier.BRONZE)) {
                    Tier downgraded = previous.previous();
                    reward.setTier(downgraded);
                    // Drop the balance to the floor of the lower tier so points
                    // and tier stay consistent.
                    reward.setPoints((long) downgraded.getMinPoints());
                    if (user != null && user.getEmail() != null) {
                        emailService.sendTierDowngradeEmail(
                                user.getEmail(), user.getName(), previous, downgraded, toResponse(reward));
                    }
                    log.info("Downgraded account {} from {} to {} after {} days inactive",
                            reward.getAccountId(), previous, downgraded, daysInactive);
                }
                // Restart the clock either way so we don't re-process every run.
                reward.setLastEarnedDate(now);
                reward.setDowngradeWarningSent(false);
                rewardAccountRepository.save(reward);
            } else if (!Boolean.TRUE.equals(reward.getDowngradeWarningSent())
                    && !reward.getTier().equals(Tier.BRONZE)) {
                long daysRemaining = INACTIVITY_DOWNGRADE_DAYS - daysInactive;
                if (user != null && user.getEmail() != null) {
                    emailService.sendDowngradeWarningEmail(
                            user.getEmail(), user.getName(), reward.getTier(),
                            daysRemaining, toResponse(reward));
                }
                reward.setDowngradeWarningSent(true);
                rewardAccountRepository.save(reward);
                log.info("Sent downgrade warning to account {} ({} days remaining)",
                        reward.getAccountId(), daysRemaining);
            }
        }
    }

    /**
     * Emails every active reward holder a snapshot of their standing.
     */
    @Transactional(readOnly = true)
    public void sendMonthlySummaries() {
        var accounts = rewardAccountRepository.findAll();
        log.info("Sending monthly reward summaries to {} account(s)", accounts.size());
        for (RewardAccount reward : accounts) {
            AppUser user = reward.getUserId() != null
                    ? userRepository.findById(reward.getUserId()).orElse(null)
                    : userRepository.findByAccountId(reward.getAccountId()).orElse(null);
            if (user != null && user.getEmail() != null) {
                emailService.sendMonthlySummaryEmail(user.getEmail(), user.getName(), toResponse(reward));
            }
        }
    }

    // ─── HELPERS ─────────────────────────────────────────────────────

    private boolean isNearNextTier(RewardAccount reward) {
        Tier tier = Tier.fromPoints(reward.getPoints());
        Tier next = tier.next();
        if (next == null) {
            return false;
        }
        long toNext = next.getMinPoints() - reward.getPoints();
        return toNext > 0 && toNext <= NEAR_TIER_THRESHOLD;
    }

    private RewardResponse toResponse(RewardAccount reward) {
        long points = reward.getPoints();
        Tier tier = Tier.fromPoints(points);
        Tier next = tier.next();

        long pointsToNext = next != null ? Math.max(0, next.getMinPoints() - points) : 0;

        int progressPercent;
        if (next == null) {
            progressPercent = 100;
        } else {
            long span = (long) next.getMinPoints() - tier.getMinPoints();
            long into = points - tier.getMinPoints();
            progressPercent = span > 0 ? (int) Math.min(100, Math.max(0, (into * 100) / span)) : 0;
        }

        return RewardResponse.builder()
                .accountId(reward.getAccountId())
                .points(points)
                .lifetimePoints(reward.getLifetimePoints())
                .tier(tier.name())
                .tierName(tier.getDisplayName())
                .tierIcon(tier.getIcon())
                .tierColor(tier.getColor())
                .multiplier(tier.getMultiplier())
                .nextTier(next != null ? next.getDisplayName() : null)
                .pointsToNextTier(pointsToNext)
                .progressPercent(progressPercent)
                .build();
    }
}
