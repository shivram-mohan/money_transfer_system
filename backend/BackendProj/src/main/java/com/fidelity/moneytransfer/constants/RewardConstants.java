package com.fidelity.moneytransfer.constants;

import java.math.BigDecimal;

/**
 * Central constants for the rewards module so the rules live in one place
 * instead of being scattered across services.
 */
public final class RewardConstants {

    private RewardConstants() {
    }

    /**
     * Fixed id of the corporate CASHBACK account that funds all redemptions. It
     * sits at the very top of the 10-digit account-id space, which
     * {@code AccountService.generateUniqueAccountId()} never produces (its bound
     * is exclusive), so it can never collide with a real account.
     */
    public static final long CASHBACK_ACCOUNT_ID = 9_999_999_999L;

    public static final String CASHBACK_ACCOUNT_NAME = "CASHBACK";

    /** One reward point is earned per this many units transferred. */
    public static final BigDecimal POINTS_EARN_DIVISOR = new BigDecimal("100");

    /** A transfer amount must exceed this to earn any reward. */
    public static final BigDecimal MIN_REWARDABLE_AMOUNT = new BigDecimal("100");

    /** Points are doubled on weekends (Saturday & Sunday). */
    public static final long WEEKEND_MULTIPLIER = 2;

    /** Minimum point balance required before cash redemption is allowed. */
    public static final long REDEEM_THRESHOLD = 500;

    /** Corporate cashback float. */
    public static final BigDecimal CASHBACK_SEED_BALANCE = new BigDecimal("1000000000.00");
}
