package com.fidelity.moneytransfer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Outcome of processing rewards for a single transfer. Surfaced to the client
 * (via {@link TransferResponse}) so the UI can celebrate points earned.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RewardResult {

    /** False when the transfer earned nothing (amount too small, not a
     *  registered sender, or the daily reward for this recipient was used). */
    private boolean rewarded;

    private long pointsEarned;
    private boolean weekendBonus;

    private long pointsBalance;
    private long lifetimePoints;

    /** True when this transfer pushed the balance to the redeemable threshold. */
    private boolean reachedRedeemThreshold;

    /** Human-friendly explanation, e.g. why nothing was earned. */
    private String message;

    public static RewardResult none(String message) {
        return RewardResult.builder()
                .rewarded(false)
                .pointsEarned(0)
                .message(message)
                .build();
    }
}
