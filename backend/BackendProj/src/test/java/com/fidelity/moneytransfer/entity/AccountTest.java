package com.fidelity.moneytransfer.entity;

import com.fidelity.moneytransfer.enums.AccountStatus;
import com.fidelity.moneytransfer.exception.InsufficientBalanceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    private Account account;

    @BeforeEach
    void setUp() {
        account = Account.builder()
                .id(1L)
                .holderName("John Doe")
                .balance(new BigDecimal("1000.00"))
                .status(AccountStatus.ACTIVE)
                .version(0)
                .build();
    }

    @Test
    void testDebit_Success() {
        // Arrange
        BigDecimal debitAmount = new BigDecimal("300.00");
        BigDecimal expectedBalance = new BigDecimal("700.00");

        // Act
        account.debit(debitAmount);

        // Assert
        assertEquals(expectedBalance, account.getBalance());
    }

    @Test
    void testDebit_InsufficientBalance() {
        // Arrange
        BigDecimal debitAmount = new BigDecimal("1500.00");

        // Act & Assert
        assertThrows(InsufficientBalanceException.class, () -> {
            account.debit(debitAmount);
        });
    }

    @Test
    void testCredit_Success() {
        // Arrange
        BigDecimal creditAmount = new BigDecimal("500.00");
        BigDecimal expectedBalance = new BigDecimal("1500.00");

        // Act
        account.credit(creditAmount);

        // Assert
        assertEquals(expectedBalance, account.getBalance());
    }

    @Test
    void testIsActive_WhenActive() {
        // Arrange
        account.setStatus(AccountStatus.ACTIVE);

        // Act & Assert
        assertTrue(account.isActive());
    }

    @Test
    void testIsActive_WhenLocked() {
        // Arrange
        account.setStatus(AccountStatus.LOCKED);

        // Act & Assert
        assertFalse(account.isActive());
    }

    @Test
    void testIsActive_WhenClosed() {
        // Arrange
        account.setStatus(AccountStatus.CLOSED);

        // Act & Assert
        assertFalse(account.isActive());
    }
}