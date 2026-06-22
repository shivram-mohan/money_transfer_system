package com.fidelity.moneytransfer.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers the small bits of hand-written logic in DTOs (static factories etc.);
 * the Lombok-generated accessors are excluded from coverage via lombok.config.
 */
class DtoLogicTest {

    @Test
    void rewardResult_none_BuildsUnrewardedResult() {
        RewardResult r = RewardResult.none("nope");
        assertFalse(r.isRewarded());
        assertEquals(0, r.getPointsEarned());
        assertEquals(BigDecimal.ZERO, r.getCashbackAmount());
        assertEquals("nope", r.getMessage());
    }

    @Test
    void transactionFilterRequest_lastWeek() {
        TransactionFilterRequest f = TransactionFilterRequest.lastWeek(1L);
        assertEquals(1L, f.getAccountId());
        assertEquals("LAST_WEEK", f.getFilterType());
        assertEquals(LocalDate.now().minusWeeks(1), f.getStartDate());
        assertEquals(LocalDate.now(), f.getEndDate());
    }

    @Test
    void transactionFilterRequest_lastMonth() {
        TransactionFilterRequest f = TransactionFilterRequest.lastMonth(2L);
        assertEquals("LAST_MONTH", f.getFilterType());
        assertEquals(LocalDate.now().minusMonths(1), f.getStartDate());
    }

    @Test
    void transactionFilterRequest_lastYear() {
        TransactionFilterRequest f = TransactionFilterRequest.lastYear(3L);
        assertEquals("LAST_YEAR", f.getFilterType());
        assertEquals(LocalDate.now().minusYears(1), f.getStartDate());
    }

    @Test
    void transactionFilterRequest_custom() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 2, 1);
        TransactionFilterRequest f = TransactionFilterRequest.custom(4L, start, end);
        assertEquals("CUSTOM", f.getFilterType());
        assertEquals(start, f.getStartDate());
        assertEquals(end, f.getEndDate());
    }
}
