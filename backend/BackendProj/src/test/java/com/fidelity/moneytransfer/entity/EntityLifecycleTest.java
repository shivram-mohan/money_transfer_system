package com.fidelity.moneytransfer.entity;

import com.fidelity.moneytransfer.enums.TransactionStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises the JPA lifecycle callbacks and small helper methods that Lombok
 * does NOT generate (and which therefore count toward coverage).
 */
class EntityLifecycleTest {

    @Test
    void appUser_prePersist_DefaultsRewardFields() {
        AppUser user = AppUser.builder()
                .username("john").password("p").name("John")
                .role("USER").status("ACTIVE").build();

        user.prePersist();

        assertNotNull(user.getCreatedDate());
        assertNotNull(user.getLastModifiedDate());
        assertEquals(0L, user.getRewardPoints());
        assertEquals("BRONZE", user.getTier());
        assertEquals(Boolean.FALSE, user.getDowngradeWarningSent());
    }

    @Test
    void appUser_prePersist_KeepsProvidedValues() {
        AppUser user = AppUser.builder()
                .username("john").password("p").name("John").role("USER").status("ACTIVE")
                .rewardPoints(750L).tier("SILVER").downgradeWarningSent(true).build();

        user.prePersist();

        assertEquals(750L, user.getRewardPoints());
        assertEquals("SILVER", user.getTier());
        assertTrue(user.getDowngradeWarningSent());
    }

    @Test
    void appUser_preUpdate_TouchesModifiedDate() {
        AppUser user = AppUser.builder()
                .username("john").password("p").name("John").role("USER").status("ACTIVE").build();
        user.preUpdate();
        assertNotNull(user.getLastModifiedDate());
    }

    @Test
    void otpToken_touch_AndExpiry() {
        OtpToken token = OtpToken.builder()
                .email("e@x.com").otp("123456").purpose("LOGIN")
                .expiresAt(LocalDateTime.now().plusMinutes(5)).build();
        token.touch();
        assertNotNull(token.getCreatedAt());
        assertFalse(token.isExpired());

        token.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        assertTrue(token.isExpired());
    }

    @Test
    void transactionLog_prePersist_GeneratesIdAndTimestamp() {
        TransactionLog log = TransactionLog.builder()
                .fromAccountId(1L).toAccountId(2L).amount(new BigDecimal("10.00"))
                .status(TransactionStatus.SUCCESS).build();
        log.prePersist();
        assertNotNull(log.getId());
        assertNotNull(log.getCreatedOn());
    }

    @Test
    void transactionLog_prePersist_KeepsExistingId() {
        TransactionLog log = TransactionLog.builder()
                .id("fixed-id").fromAccountId(1L).toAccountId(2L)
                .amount(new BigDecimal("10.00")).status(TransactionStatus.SUCCESS)
                .createdOn(LocalDateTime.of(2026, 1, 1, 0, 0)).build();
        log.prePersist();
        assertEquals("fixed-id", log.getId());
        assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0), log.getCreatedOn());
    }

    @Test
    void rewardLedger_prePersist_DefaultsDates() {
        RewardLedger ledger = RewardLedger.builder()
                .userId(1L).fromAccountId(1L).toAccountId(2L)
                .pointsEarned(10L).cashbackAmount(new BigDecimal("5.00")).build();
        ledger.prePersist();
        assertNotNull(ledger.getCreatedOn());
        assertEquals(ledger.getCreatedOn().toLocalDate(), ledger.getRewardDate());
    }

    @Test
    void rewardLedger_prePersist_KeepsProvidedDates() {
        LocalDateTime created = LocalDateTime.of(2026, 3, 3, 9, 0);
        RewardLedger ledger = RewardLedger.builder()
                .userId(1L).fromAccountId(1L).toAccountId(2L).pointsEarned(1L)
                .createdOn(created).rewardDate(created.toLocalDate()).build();
        ledger.prePersist();
        assertEquals(created, ledger.getCreatedOn());
    }

    @Test
    void account_updateTimestamp() {
        Account account = Account.builder()
                .id(1L).holderName("John").balance(new BigDecimal("10.00"))
                .status(com.fidelity.moneytransfer.enums.AccountStatus.ACTIVE)
                .version(0).build();
        account.updateTimestamp();
        assertNotNull(account.getLastUpdated());
    }
}
