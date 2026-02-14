package com.fidelity.moneytransfer.entity;

import com.fidelity.moneytransfer.enums.AccountStatus;
import com.fidelity.moneytransfer.enums.AccountType;
import com.fidelity.moneytransfer.exception.InsufficientBalanceException;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "holder_name", nullable = false)
    private String holderName;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    // ✅ NEW - Account Type
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    @Builder.Default
    private AccountType accountType = AccountType.SAVINGS;

    // ✅ NEW - Monthly transaction count
    @Column(name = "monthly_transaction_count")
    @Builder.Default
    private Integer monthlyTransactionCount = 0;

    // ✅ NEW - Last transaction reset date
    @Column(name = "last_transaction_reset")
    private LocalDateTime lastTransactionReset;

    // ✅ NEW - Daily withdrawal amount
    @Column(name = "daily_withdrawal_amount", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal dailyWithdrawalAmount = BigDecimal.ZERO;

    // ✅ NEW - Last withdrawal reset date
    @Column(name = "last_withdrawal_reset")
    private LocalDateTime lastWithdrawalReset;

    @Version
    private Integer version;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    // ============ BUSINESS LOGIC METHODS ============

    public void credit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }
        this.balance = this.balance.add(amount);
        this.lastUpdated = LocalDateTime.now();
    }

    public void debit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debit amount must be positive");
        }

        // ✅ NEW - Check overdraft limit for current accounts
        BigDecimal minimumAllowedBalance = getMinimumAllowedBalance();

        if (this.balance.subtract(amount).compareTo(minimumAllowedBalance) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance. Available: " + this.balance +
                            ", Required: " + amount +
                            ", Minimum allowed: " + minimumAllowedBalance
            );
        }

        this.balance = this.balance.subtract(amount);
        this.lastUpdated = LocalDateTime.now();
    }

    public boolean isActive() {
        return this.status == AccountStatus.ACTIVE;
    }

    // ✅ NEW - Get minimum allowed balance (overdraft for current)
    public BigDecimal getMinimumAllowedBalance() {
        if (accountType == AccountType.CURRENT) {
            return new BigDecimal("-10000"); // ₹10,000 overdraft allowed
        }
        return BigDecimal.ZERO; // No overdraft for savings
    }

    // ✅ NEW - Get minimum balance requirement
    public BigDecimal getMinimumBalanceRequirement() {
        if (accountType == AccountType.SAVINGS) {
            return new BigDecimal("1000");
        }
        return new BigDecimal("5000");
    }

    // ✅ NEW - Get monthly transaction limit
    public Integer getMonthlyTransactionLimit() {
        if (accountType == AccountType.SAVINGS) {
            return 10;
        }
        return null; // Unlimited for current accounts
    }

    // ✅ NEW - Get daily withdrawal limit
    public BigDecimal getDailyWithdrawalLimit() {
        if (accountType == AccountType.SAVINGS) {
            return new BigDecimal("50000");
        }
        return null; // Unlimited for current accounts
    }

    // ✅ NEW - Increment transaction count
    public void incrementTransactionCount() {
        resetCountersIfNeeded();
        this.monthlyTransactionCount++;
    }

    // ✅ NEW - Add to daily withdrawal
    public void addToDailyWithdrawal(BigDecimal amount) {
        resetCountersIfNeeded();
        if (this.dailyWithdrawalAmount == null) {
            this.dailyWithdrawalAmount = BigDecimal.ZERO;
        }
        this.dailyWithdrawalAmount = this.dailyWithdrawalAmount.add(amount);
    }

    // ✅ NEW - Reset counters if month/day has changed
    private void resetCountersIfNeeded() {
        LocalDateTime now = LocalDateTime.now();

        // Reset monthly counter
        if (lastTransactionReset == null ||
                lastTransactionReset.getMonth() != now.getMonth() ||
                lastTransactionReset.getYear() != now.getYear()) {
            this.monthlyTransactionCount = 0;
            this.lastTransactionReset = now;
        }

        // Reset daily counter
        if (lastWithdrawalReset == null ||
                lastWithdrawalReset.toLocalDate().isBefore(now.toLocalDate())) {
            this.dailyWithdrawalAmount = BigDecimal.ZERO;
            this.lastWithdrawalReset = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.lastUpdated = LocalDateTime.now();
    }
}