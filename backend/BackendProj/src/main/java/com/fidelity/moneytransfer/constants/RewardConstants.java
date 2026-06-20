package com.fidelity.moneytransfer.constants;

import java.math.BigDecimal;

/**
 * Central constants for the rewards module so the rules live in one place
 * (and the magic numbers don't get scattered across services).
 */
public final class RewardConstants {

    private RewardConstants() {
    }

    /**
     * Fixed id of the corporate CASHBACK account that funds all cashback
     * payouts. It sits at the very top of the 10-digit account-id space, which
     * {@code AccountService.generateUniqueAccountId()} never produces (its bound
     * is exclusive), so it can never collide with a real account.
     */
    public static final long CASHBACK_ACCOUNT_ID = 9_999_999_999L;

    public static final String CASHBACK_ACCOUNT_NAME = "CASHBACK";

    /** One reward point is earned per this many units transferred. */
    public static final BigDecimal POINTS_EARN_DIVISOR = new BigDecimal("100");

    /** A transfer must exceed this amount to earn any reward. */
    public static final BigDecimal MIN_REWARDABLE_AMOUNT = new BigDecimal("100");

    /** Corporate cashback float. */
    public static final BigDecimal CASHBACK_SEED_BALANCE = new BigDecimal("1000000000.00");

    /** A user within this many points of the next tier is "close" and is nudged. */
    public static final long CLOSE_TO_NEXT_TIER_THRESHOLD = 100;

    /** Inactivity (in days) after which a user's tier is downgraded. */
    public static final long INACTIVITY_DOWNGRADE_DAYS = 30;

    /** How many days before the downgrade the warning email is sent. */
    public static final long DOWNGRADE_WARNING_LEAD_DAYS = 10;
}
