package com.fidelity.moneytransfer.service;

import com.fidelity.moneytransfer.constants.RewardConstants;
import com.fidelity.moneytransfer.dto.MonthlySummaryResponse;
import com.fidelity.moneytransfer.entity.AppUser;
import com.fidelity.moneytransfer.enums.Tier;
import com.fidelity.moneytransfer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Time-based rewards communications:
 * <ul>
 *   <li>Monthly summary email on the 1st of each month.</li>
 *   <li>Daily inactivity sweep: warns 10 days before, and downgrades one tier
 *       after 30 days of no sent transfers.</li>
 * </ul>
 * Each user is processed defensively so one failure never aborts the batch.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RewardSchedulerService {

    private final UserRepository userRepository;
    private final RewardService rewardService;
    private final EmailService emailService;

    /** 08:00 on the 1st of every month — summary for the month that just ended. */
    @Scheduled(cron = "0 0 8 1 * *")
    @Transactional(readOnly = true)
    public void sendMonthlySummaries() {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        log.info("Generating monthly reward summaries for {}", lastMonth);

        for (AppUser user : eligibleUsers()) {
            try {
                MonthlySummaryResponse summary = rewardService.buildMonthlySummary(user, lastMonth);
                emailService.sendMonthlySummaryEmail(
                        user.getEmail(), user.getName(), summary.getMonth(),
                        summary.getRewardedTransfers(), summary.getPointsEarned(),
                        summary.getCashbackEarned().toPlainString(),
                        summary.getTier(), summary.getTotalPoints());
            } catch (Exception e) {
                log.error("Monthly summary failed for user {}: {}", user.getId(), e.getMessage());
            }
        }
    }

    /** 09:00 daily — pre-downgrade warnings and inactivity downgrades. */
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void runInactivitySweep() {
        log.info("Running rewards inactivity sweep");
        LocalDate today = LocalDate.now();

        for (AppUser user : eligibleUsers()) {
            try {
                processInactivity(user, today);
            } catch (Exception e) {
                log.error("Inactivity processing failed for user {}: {}", user.getId(), e.getMessage());
            }
        }
    }

    private void processInactivity(AppUser user, LocalDate today) {
        LocalDateTime last = user.getLastTransactionDate();
        if (last == null) {
            return; // never transacted — nothing to downgrade from
        }
        Tier currentTier = Tier.fromPoints(user.getRewardPoints() != null ? user.getRewardPoints() : 0L);
        if (currentTier == Tier.BRONZE) {
            return; // already at the bottom
        }

        long daysInactive = ChronoUnit.DAYS.between(last.toLocalDate(), today);

        if (daysInactive >= RewardConstants.INACTIVITY_DOWNGRADE_DAYS) {
            Tier newTier = currentTier.previous();
            user.setRewardPoints(newTier.getMinPoints());
            user.setTier(newTier.name());
            // Restart the clock so the user isn't downgraded again the next day.
            user.setLastTransactionDate(LocalDateTime.now());
            user.setDowngradeWarningSent(false);
            userRepository.save(user);
            emailService.sendDowngradeEmail(user.getEmail(), user.getName(),
                    currentTier.name(), newTier.name());
            log.info("Downgraded user {} from {} to {} (inactive {} days)",
                    user.getId(), currentTier, newTier, daysInactive);
            return;
        }

        long warnAt = RewardConstants.INACTIVITY_DOWNGRADE_DAYS
                - RewardConstants.DOWNGRADE_WARNING_LEAD_DAYS;
        if (daysInactive >= warnAt && !Boolean.TRUE.equals(user.getDowngradeWarningSent())) {
            long daysUntilDowngrade = RewardConstants.INACTIVITY_DOWNGRADE_DAYS - daysInactive;
            user.setDowngradeWarningSent(true);
            userRepository.save(user);
            emailService.sendDowngradeWarningEmail(user.getEmail(), user.getName(),
                    currentTier.name(), daysUntilDowngrade);
            log.info("Sent downgrade warning to user {} ({} days until downgrade)",
                    user.getId(), daysUntilDowngrade);
        }
    }

    /** Active, bank-linked users with an email address. */
    private List<AppUser> eligibleUsers() {
        return userRepository.findByStatus("ACTIVE").stream()
                .filter(u -> u.getAccountId() != null)
                .filter(u -> u.getEmail() != null && !u.getEmail().isBlank())
                .toList();
    }
}
