package com.fidelity.moneytransfer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A user's current rewards standing, shown on the profile/rewards page. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RewardProfileResponse {

    private String holderName;
    private long totalPoints;

    private String tier;
    private BigDecimal multiplier;

    private String nextTier;
    private long pointsToNextTier;
    /** 0-100 progress through the current tier toward the next one. */
    private int progressPercent;

    private LocalDateTime lastTransactionDate;

    // Lifetime totals from the ledger.
    private BigDecimal totalCashbackEarned;
}
