package com.fidelity.moneytransfer.enums;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class TierTest {

    @Test
    void fromPoints_ResolvesCorrectBand() {
        assertEquals(Tier.BRONZE, Tier.fromPoints(0));
        assertEquals(Tier.BRONZE, Tier.fromPoints(499));
        assertEquals(Tier.SILVER, Tier.fromPoints(500));
        assertEquals(Tier.SILVER, Tier.fromPoints(1999));
        assertEquals(Tier.GOLD, Tier.fromPoints(2000));
        assertEquals(Tier.GOLD, Tier.fromPoints(4999));
        assertEquals(Tier.PLATINUM, Tier.fromPoints(5000));
        assertEquals(Tier.PLATINUM, Tier.fromPoints(1_000_000));
    }

    @Test
    void fromPoints_NegativeClampsToBronze() {
        assertEquals(Tier.BRONZE, Tier.fromPoints(-100));
    }

    @Test
    void multipliersAndMinPoints() {
        assertEquals(new BigDecimal("1.0"), Tier.BRONZE.getMultiplier());
        assertEquals(new BigDecimal("1.5"), Tier.SILVER.getMultiplier());
        assertEquals(new BigDecimal("2.0"), Tier.GOLD.getMultiplier());
        assertEquals(new BigDecimal("3.0"), Tier.PLATINUM.getMultiplier());
        assertEquals(0L, Tier.BRONZE.getMinPoints());
        assertEquals(5000L, Tier.PLATINUM.getMinPoints());
    }

    @Test
    void next_WalksUpAndStopsAtPlatinum() {
        assertEquals(Tier.SILVER, Tier.BRONZE.next());
        assertEquals(Tier.GOLD, Tier.SILVER.next());
        assertEquals(Tier.PLATINUM, Tier.GOLD.next());
        assertNull(Tier.PLATINUM.next());
    }

    @Test
    void previous_WalksDownAndFloorsAtBronze() {
        assertEquals(Tier.GOLD, Tier.PLATINUM.previous());
        assertEquals(Tier.SILVER, Tier.GOLD.previous());
        assertEquals(Tier.BRONZE, Tier.SILVER.previous());
        assertEquals(Tier.BRONZE, Tier.BRONZE.previous());
    }

    @Test
    void pointsToNext_ComputesRemainder_AndZeroAtTop() {
        assertEquals(500L, Tier.BRONZE.pointsToNext(0));
        assertEquals(1L, Tier.BRONZE.pointsToNext(499));
        assertEquals(0L, Tier.SILVER.pointsToNext(2000)); // already past, clamped to 0
        assertEquals(0L, Tier.PLATINUM.pointsToNext(9999));
    }
}
