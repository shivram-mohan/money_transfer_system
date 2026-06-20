package com.fidelity.moneytransfer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Outcome of processing rewards for a single transfer. Surfaced to the client
 * (via {@link TransferResponse}) so the UI can celebrate points, cashback and
 * tier changes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RewardResult {

    /** False when the transfer earned nothing (e.g. amount too small, or the
     *  daily reward for this recipient was already claimed). */
    private boolean rewarded;

    private long pointsEarned;
    private BigDecimal cashbackAmount;

    private long totalPoints;
    private String tier;
    private String nextTier;
    private long pointsToNextTier;

    private boolean tierUpgraded;
    private boolean closeToNextTier;

    /** Human-friendly explanation, e.g. why nothing was earned. */
    private String message;

    public static RewardResult none(String message) {
        return RewardResult.builder()
                .rewarded(false)
                .pointsEarned(0)
                .cashbackAmount(BigDecimal.ZERO)
                .message(message)
                .build();
    }
}
