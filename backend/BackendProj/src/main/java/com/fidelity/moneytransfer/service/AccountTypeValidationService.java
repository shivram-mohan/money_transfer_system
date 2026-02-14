package com.fidelity.moneytransfer.service;

import com.fidelity.moneytransfer.entity.Account;
import com.fidelity.moneytransfer.enums.AccountType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class AccountTypeValidationService {

    /**
     * Validate if account can perform a debit transaction
     */
    public void validateDebitTransaction(Account account, BigDecimal amount) {
        log.debug("Validating debit for account type: {}", account.getAccountType());

        // Check monthly transaction limit (Savings only)
        if (account.getAccountType() == AccountType.SAVINGS) {
            Integer limit = account.getMonthlyTransactionLimit();
            if (limit != null && account.getMonthlyTransactionCount() >= limit) {
                throw new IllegalStateException(
                        "Monthly transaction limit exceeded. " +
                                "Savings accounts are limited to " + limit +
                                " transactions per month."
                );
            }
        }

        // Check daily withdrawal limit (Savings only)
        if (account.getAccountType() == AccountType.SAVINGS) {
            BigDecimal dailyLimit = account.getDailyWithdrawalLimit();
            if (dailyLimit != null) {
                BigDecimal currentWithdrawal = account.getDailyWithdrawalAmount() != null ?
                        account.getDailyWithdrawalAmount() : BigDecimal.ZERO;

                if (currentWithdrawal.add(amount).compareTo(dailyLimit) > 0) {
                    throw new IllegalStateException(
                            "Daily withdrawal limit exceeded. " +
                                    "Savings accounts are limited to ₹" + dailyLimit +
                                    " per day. Already withdrawn: ₹" + currentWithdrawal
                    );
                }
            }
        }

        // Check minimum balance after debit
        BigDecimal balanceAfterDebit = account.getBalance().subtract(amount);
        BigDecimal minimumAllowed = account.getMinimumAllowedBalance();

        if (balanceAfterDebit.compareTo(minimumAllowed) < 0) {
            String message = account.getAccountType() == AccountType.CURRENT ?
                    "Overdraft limit exceeded. Maximum overdraft: ₹10,000" :
                    "Insufficient balance. Minimum balance required: ₹1,000";

            throw new IllegalStateException(message);
        }
    }

    /**
     * Validate if account can perform a credit transaction
     */
    public void validateCreditTransaction(Account account, BigDecimal amount) {
        // Credit transactions don't have special restrictions
        log.debug("Credit transaction validated for account type: {}",
                account.getAccountType());
    }

    /**
     * Validate minimum balance for account creation
     */
    public void validateMinimumBalance(AccountType accountType, BigDecimal initialBalance) {
        BigDecimal minimumRequired = accountType == AccountType.SAVINGS ?
                new BigDecimal("1000") : new BigDecimal("5000");

        if (initialBalance.compareTo(minimumRequired) < 0) {
            throw new IllegalArgumentException(
                    accountType + " account requires minimum balance of ₹" +
                            minimumRequired + ". Provided: ₹" + initialBalance
            );
        }
    }
}