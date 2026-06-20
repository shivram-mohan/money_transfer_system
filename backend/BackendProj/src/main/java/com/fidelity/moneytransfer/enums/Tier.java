package com.fidelity.moneytransfer.enums;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Reward tiers. A user's tier is derived purely from their lifetime reward
 * points, so points are the single source of truth and the tier is always
 * consistent with them.
 *
 * <ul>
 *   <li>BRONZE   : 0    - 499   (1.0x points multiplier)</li>
 *   <li>SILVER   : 500  - 1999  (1.5x)</li>
 *   <li>GOLD     : 2000 - 4999  (2.0x)</li>
 *   <li>PLATINUM : 5000+        (3.0x)</li>
 * </ul>
 */
public enum Tier {

    BRONZE(0, new BigDecimal("1.0")),
    SILVER(500, new BigDecimal("1.5")),
    GOLD(2000, new BigDecimal("2.0")),
    PLATINUM(5000, new BigDecimal("3.0"));

    private final long minPoints;
    private final BigDecimal multiplier;

    Tier(long minPoints, BigDecimal multiplier) {
        this.minPoints = minPoints;
        this.multiplier = multiplier;
    }

    public long getMinPoints() {
        return minPoints;
    }

    public BigDecimal getMultiplier() {
        return multiplier;
    }

    /** Resolves the tier a given (lifetime) point total falls into. */
    public static Tier fromPoints(long points) {
        long safePoints = Math.max(points, 0);
        return Arrays.stream(values())
                .sorted(Comparator.comparingLong(Tier::getMinPoints).reversed())
                .filter(tier -> safePoints >= tier.minPoints)
                .findFirst()
                .orElse(BRONZE);
    }

    /** The next tier up, or {@code null} when already at the top (PLATINUM). */
    public Tier next() {
        int idx = ordinal();
        return idx < values().length - 1 ? values()[idx + 1] : null;
    }

    /** The next tier down, or BRONZE when already at the bottom. */
    public Tier previous() {
        int idx = ordinal();
        return idx > 0 ? values()[idx - 1] : BRONZE;
    }

    /** Points still required to reach the next tier; 0 when at PLATINUM. */
    public long pointsToNext(long currentPoints) {
        Tier next = next();
        if (next == null) {
            return 0;
        }
        return Math.max(next.minPoints - currentPoints, 0);
    }
}
