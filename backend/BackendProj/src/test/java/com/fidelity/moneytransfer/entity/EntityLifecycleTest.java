package com.fidelity.moneytransfer.entity;

import com.fidelity.moneytransfer.enums.AccountStatus;
import com.fidelity.moneytransfer.enums.TransactionStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises the JPA lifecycle callbacks (@PrePersist / @PreUpdate) and the
 * small business helpers that live on the entities.
 */
class EntityLifecycleTest {

    // ─── Account ──────────────────────────────────────────────────────

    @Test
    void account_updateTimestamp_setsLastUpdated() {
        Account a = Account.builder().id(1L).holderName("X")
                .balance(BigDecimal.TEN).status(AccountStatus.ACTIVE).version(0).build();
        assertNull(a.getLastUpdated());
        a.updateTimestamp();
        assertNotNull(a.getLastUpdated());
    }

    @Test
    void account_creditAndDebit() {
        Account a = Account.builder().id(1L).holderName("X")
                .balance(new BigDecimal("100.00")).status(AccountStatus.ACTIVE).build();
        a.credit(new BigDecimal("50.00"));
        assertEquals(new BigDecimal("150.00"), a.getBalance());
        a.debit(new BigDecimal("30.00"));
        assertEquals(new BigDecimal("120.00"), a.getBalance());
    }

    // ─── AppUser ──────────────────────────────────────────────────────

    @Test
    void appUser_prePersist_defaultsRewardPointsWhenNull() {
        AppUser u = AppUser.builder().username("a").password("p").name("n")
                .role("USER").status("ACTIVE").build();
        u.prePersist();
        assertNotNull(u.getCreatedDate());
        assertNotNull(u.getLastModifiedDate());
        assertEquals(0L, u.getRewardPoints());
        assertEquals(0L, u.getLifetimeRewardPoints());
    }

    @Test
    void appUser_prePersist_keepsExistingRewardPoints() {
        AppUser u = AppUser.builder().username("a").password("p").name("n")
                .role("USER").status("ACTIVE").rewardPoints(5L).lifetimeRewardPoints(9L).build();
        u.prePersist();
        assertEquals(5L, u.getRewardPoints());
        assertEquals(9L, u.getLifetimeRewardPoints());
    }

    @Test
    void appUser_preUpdate_setsLastModified() {
        AppUser u = AppUser.builder().username("a").password("p").name("n")
                .role("USER").status("ACTIVE").build();
        u.preUpdate();
        assertNotNull(u.getLastModifiedDate());
    }

    // ─── TransactionLog ───────────────────────────────────────────────

    @Test
    void transactionLog_prePersist_generatesIdAndTimestamp() {
        TransactionLog t = TransactionLog.builder().fromAccountId(1L).toAccountId(2L)
                .amount(BigDecimal.ONE).status(TransactionStatus.SUCCESS).build();
        t.prePersist();
        assertNotNull(t.getId());
        assertNotNull(t.getCreatedOn());
    }

    @Test
    void transactionLog_prePersist_keepsExistingIdAndTimestamp() {
        LocalDateTime ts = LocalDateTime.of(2024, 1, 1, 0, 0);
        TransactionLog t = TransactionLog.builder().id("fixed").fromAccountId(1L)
                .toAccountId(2L).amount(BigDecimal.ONE).status(TransactionStatus.SUCCESS)
                .createdOn(ts).build();
        t.prePersist();
        assertEquals("fixed", t.getId());
        assertEquals(ts, t.getCreatedOn());
    }

    // ─── OtpToken ─────────────────────────────────────────────────────

    @Test
    void otpToken_touch_setsCreatedAt_andExpiry() {
        OtpToken token = OtpToken.builder().email("a@b.com").otp("123456")
                .purpose("LOGIN").expiresAt(LocalDateTime.now().plusMinutes(5)).build();
        token.touch();
        assertNotNull(token.getCreatedAt());
        assertFalse(token.isExpired());
    }

    @Test
    void otpToken_isExpired_true() {
        OtpToken token = OtpToken.builder().email("a@b.com").otp("123456")
                .purpose("LOGIN").expiresAt(LocalDateTime.now().minusSeconds(1)).build();
        assertTrue(token.isExpired());
    }

    // ─── RewardLedger ─────────────────────────────────────────────────

    @Test
    void rewardLedger_prePersist_setsDefaults() {
        RewardLedger r = RewardLedger.builder().userId(1L).fromAccountId(1L)
                .toAccountId(2L).pointsEarned(10L).build();
        r.prePersist();
        assertNotNull(r.getCreatedOn());
        assertNotNull(r.getRewardDate());
        assertEquals(r.getCreatedOn().toLocalDate(), r.getRewardDate());
        assertFalse(r.getWeekendBonus());
    }

    @Test
    void rewardLedger_prePersist_keepsExistingValues() {
        LocalDateTime created = LocalDateTime.of(2024, 6, 1, 10, 0);
        LocalDate date = LocalDate.of(2024, 6, 1);
        RewardLedger r = RewardLedger.builder().userId(1L).fromAccountId(1L).toAccountId(2L)
                .pointsEarned(10L).weekendBonus(true).createdOn(created).rewardDate(date).build();
        r.prePersist();
        assertEquals(created, r.getCreatedOn());
        assertEquals(date, r.getRewardDate());
        assertTrue(r.getWeekendBonus());
    }
}
