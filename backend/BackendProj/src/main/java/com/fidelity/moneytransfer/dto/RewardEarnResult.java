package com.fidelity.moneytransfer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Outcome of awarding reward points for a single transfer. Returned alongside
 * the transfer response so the frontend can show a celebratory popup, flag a
 * tier upgrade, or nudge the user when they are close to the next tier.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardEarnResult {

    /** True when this transfer actually earned points (passed the basic rules). */
    private boolean earned;

    /** Points credited for this transfer (after the tier multiplier). */
    private long pointsEarned;

    /** Whether the tier multiplier boosted the base points. */
    private long basePoints;

    /** True if this transfer pushed the user into a higher tier. */
    private boolean tierUpgraded;

    /** True when the user is within the "almost there" threshold of the next tier. */
    private boolean nearNextTier;

    /** Current rewards snapshot after the award. */
    private RewardResponse rewards;
}
