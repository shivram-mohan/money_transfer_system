package com.fidelity.moneytransfer.dto;

import com.fidelity.moneytransfer.enums.AccountStatus;
import com.fidelity.moneytransfer.enums.TransactionStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers the hand-written (non-Lombok) members: the static factory helpers on
 * the request DTOs and the enum value lookups.
 */
class DtoAndEnumTest {

    @Test
    void rewardResult_none_buildsUnrewarded() {
        RewardResult r = RewardResult.none("nothing");
        assertFalse(r.isRewarded());
        assertEquals(0L, r.getPointsEarned());
        assertEquals("nothing", r.getMessage());
    }

    @Test
    void transactionFilter_lastWeek() {
        TransactionFilterRequest f = TransactionFilterRequest.lastWeek(1L);
        assertEquals("LAST_WEEK", f.getFilterType());
        assertEquals(LocalDate.now().minusWeeks(1), f.getStartDate());
        assertEquals(1L, f.getAccountId());
    }

    @Test
    void transactionFilter_lastMonth() {
        TransactionFilterRequest f = TransactionFilterRequest.lastMonth(1L);
        assertEquals("LAST_MONTH", f.getFilterType());
        assertEquals(LocalDate.now().minusMonths(1), f.getStartDate());
    }

    @Test
    void transactionFilter_lastYear() {
        TransactionFilterRequest f = TransactionFilterRequest.lastYear(1L);
        assertEquals("LAST_YEAR", f.getFilterType());
        assertEquals(LocalDate.now().minusYears(1), f.getStartDate());
    }

    @Test
    void transactionFilter_custom() {
        LocalDate s = LocalDate.of(2024, 1, 1);
        LocalDate e = LocalDate.of(2024, 2, 1);
        TransactionFilterRequest f = TransactionFilterRequest.custom(1L, s, e);
        assertEquals("CUSTOM", f.getFilterType());
        assertEquals(s, f.getStartDate());
        assertEquals(e, f.getEndDate());
    }

    @Test
    void accountStatus_valuesAndValueOf() {
        assertEquals(3, AccountStatus.values().length);
        assertEquals(AccountStatus.ACTIVE, AccountStatus.valueOf("ACTIVE"));
        assertEquals(AccountStatus.LOCKED, AccountStatus.valueOf("LOCKED"));
        assertEquals(AccountStatus.CLOSED, AccountStatus.valueOf("CLOSED"));
    }

    @Test
    void transactionStatus_valuesAndValueOf() {
        assertEquals(3, TransactionStatus.values().length);
        assertEquals(TransactionStatus.SUCCESS, TransactionStatus.valueOf("SUCCESS"));
        assertEquals(TransactionStatus.FAILED, TransactionStatus.valueOf("FAILED"));
        assertEquals(TransactionStatus.PENDING, TransactionStatus.valueOf("PENDING"));
    }

    @Test
    void exceptions_carryMessage() {
        assertEquals("a", new com.fidelity.moneytransfer.exception
                .AccountNotFoundException("a").getMessage());
        assertEquals("b", new com.fidelity.moneytransfer.exception
                .AccountNotActiveException("b").getMessage());
        assertEquals("c", new com.fidelity.moneytransfer.exception
                .InsufficientBalanceException("c").getMessage());
        assertEquals("d", new com.fidelity.moneytransfer.exception
                .DuplicateTransferException("d").getMessage());
    }
}
