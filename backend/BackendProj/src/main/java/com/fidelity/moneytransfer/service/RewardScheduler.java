package com.fidelity.moneytransfer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Time-based rewards jobs. Kept thin: it only decides <em>when</em> to run and
 * delegates the actual work to {@link RewardService}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RewardScheduler {

    private final RewardService rewardService;

    /**
     * Daily inactivity sweep at 01:00. Sends pre-downgrade warnings (10 days
     * out) and downgrades accounts inactive for 30+ days.
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void inactivitySweep() {
        log.info("Running daily rewards inactivity sweep");
        try {
            rewardService.processInactivity();
        } catch (Exception e) {
            log.error("Inactivity sweep failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Monthly summary emails at 09:00 on the 1st of every month.
     */
    @Scheduled(cron = "0 0 9 1 * *")
    public void monthlySummaries() {
        log.info("Running monthly rewards summary job");
        try {
            rewardService.sendMonthlySummaries();
        } catch (Exception e) {
            log.error("Monthly summary job failed: {}", e.getMessage(), e);
        }
    }
}
