package com.fidelity.moneytransfer.enums;

import java.math.BigDecimal;

/**
 * Loyalty tiers for the rewards programme. A user's tier is derived purely from
 * their current reward-point balance, so the tier is always consistent with the
 * points everywhere it is shown. Each tier carries a multiplier that is applied
 * to the base points earned on a transfer, plus display metadata used by the UI
 * and the email templates.
 */
public enum Tier {

    BRONZE("Bronze", 0, 499, new BigDecimal("1.0"), "#cd7f32", "🥉"),
    SILVER("Silver", 500, 1999, new BigDecimal("1.5"), "#9ca3af", "🥈"),
    GOLD("Gold", 2000, 4999, new BigDecimal("2.0"), "#f0b429", "🥇"),
    PLATINUM("Platinum", 5000, Integer.MAX_VALUE, new BigDecimal("3.0"), "#5eead4", "💎");

    private final String displayName;
    private final int minPoints;
    private final int maxPoints;
    private final BigDecimal multiplier;
    private final String color;
    private final String icon;

    Tier(String displayName, int minPoints, int maxPoints,
         BigDecimal multiplier, String color, String icon) {
        this.displayName = displayName;
        this.minPoints = minPoints;
        this.maxPoints = maxPoints;
        this.multiplier = multiplier;
        this.color = color;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMinPoints() {
        return minPoints;
    }

    public int getMaxPoints() {
        return maxPoints;
    }

    public BigDecimal getMultiplier() {
        return multiplier;
    }

    public String getColor() {
        return color;
    }

    public String getIcon() {
        return icon;
    }

    /**
     * Resolves the tier a given point balance falls into.
     */
    public static Tier fromPoints(long points) {
        if (points < 0) {
            points = 0;
        }
        for (Tier tier : values()) {
            if (points >= tier.minPoints && points <= tier.maxPoints) {
                return tier;
            }
        }
        return PLATINUM;
    }

    /**
     * The next tier up, or {@code null} if this is already the top tier.
     */
    public Tier next() {
        int idx = ordinal();
        return idx < values().length - 1 ? values()[idx + 1] : null;
    }

    /**
     * The next tier down, or {@code null} if this is already the bottom tier.
     */
    public Tier previous() {
        int idx = ordinal();
        return idx > 0 ? values()[idx - 1] : null;
    }

    public boolean isTopTier() {
        return next() == null;
    }
}
