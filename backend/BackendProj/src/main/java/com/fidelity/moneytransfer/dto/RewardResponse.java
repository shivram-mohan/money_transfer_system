package com.fidelity.moneytransfer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Snapshot of a user's rewards standing, used by the profile/dashboard tier
 * card. Includes everything the UI needs to render a progress bar towards the
 * next tier without doing any maths of its own.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardResponse {

    private Long accountId;
    private long points;
    private long lifetimePoints;

    private String tier;            // enum name e.g. "GOLD"
    private String tierName;        // display name e.g. "Gold"
    private String tierIcon;
    private String tierColor;
    private BigDecimal multiplier;

    private String nextTier;        // display name of next tier, null at top
    private long pointsToNextTier;  // 0 at top tier
    private int progressPercent;    // progress through the current tier, 0-100
}
