package com.fidelity.moneytransfer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A user's current rewards standing, shown on the profile/rewards page. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RewardProfileResponse {

    private String holderName;

    /** Spendable points (1 point = 1 currency on redemption). */
    private long pointsBalance;

    /** Total points ever earned. */
    private long lifetimePoints;

    private long redeemThreshold;
    private boolean canRedeem;

    /** 0-100 progress of the balance toward the redeem threshold. */
    private int progressPercent;

    private boolean weekendBonusActive;
}
