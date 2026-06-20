package com.fidelity.moneytransfer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Aggregated reward activity for a single calendar month. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySummaryResponse {

    private String month;        // e.g. "June 2026"
    private long rewardedTransfers;
    private long pointsEarned;
    private BigDecimal cashbackEarned;

    private String tier;
    private long totalPoints;
}
