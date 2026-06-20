package com.fidelity.moneytransfer.service;

import com.fidelity.moneytransfer.constants.RewardConstants;
import com.fidelity.moneytransfer.dto.AccountResponse;
import com.fidelity.moneytransfer.dto.CreateAccountRequest;
import com.fidelity.moneytransfer.entity.Account;
import com.fidelity.moneytransfer.entity.TransactionLog;
import com.fidelity.moneytransfer.enums.AccountStatus;
import com.fidelity.moneytransfer.exception.AccountNotFoundException;
import com.fidelity.moneytransfer.repository.AccountRepository;
import com.fidelity.moneytransfer.repository.TransactionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
// Add these imports at the top
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionLogRepository transactionLogRepository;

    // ─── ACCOUNT ID GENERATION ──────────────────────────────────────────

    public Long generateUniqueAccountId() {
        Long id;
        do {
            id = ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L);
        } while (accountRepository.existsById(id));
        return id;
    }

    // ─── EXISTING METHODS ─────────────────────────────────────────────

    public Account getAccountById(Long id) {
        log.debug("Fetching account with id: {}", id);
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found with id: " + id));
    }

    public AccountResponse getAccountDetails(Long id) {
        Account account = getAccountById(id);
        return mapToAccountResponse(account);
    }

    public BigDecimal getBalance(Long id) {
        Account account = getAccountById(id);
        return account.getBalance();
    }

    public List<TransactionLog> getTransactionHistory(Long accountId) {
        log.debug("Fetching transaction history for account: {}", accountId);

        // Verify account exists
        getAccountById(accountId);

        // Get transactions, newest first
        List<TransactionLog> transactions = transactionLogRepository
                .findByFromAccountIdOrToAccountIdOrderByCreatedOnDesc(accountId, accountId);

        // ✅ NEW - Populate account holder names
        transactions.forEach(txn -> {
            // Get "from" account holder name
            if (txn.getFromAccountId() != null) {
                accountRepository.findById(txn.getFromAccountId())
                        .ifPresent(account ->
                                txn.setFromAccountHolderName(account.getHolderName())
                        );
            }

            // Get "to" account holder name
            if (txn.getToAccountId() != null) {
                accountRepository.findById(txn.getToAccountId())
                        .ifPresent(account ->
                                txn.setToAccountHolderName(account.getHolderName())
                        );
            }
        });

        // Hide the corporate cashback account's id from user-facing history;
        // it surfaces only as the "CASHBACK" label.
        transactions.forEach(this::maskCashbackAccount);

        return transactions;
    }

    // ─── ADMIN METHODS ────────────────────────────────────────────────

    public List<AccountResponse> getAllAccounts() {
        log.debug("Fetching all accounts");
        return accountRepository.findAll()
                .stream()
                .map(this::mapToAccountResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        log.info("Creating new account for: {}", request.getHolderName());

        Account account = Account.builder()
                .id(generateUniqueAccountId())
                .holderName(request.getHolderName())
                .balance(request.getInitialBalance())
                .status(AccountStatus.ACTIVE)
                .version(0)
                .build();

        Account savedAccount = accountRepository.save(account);
        log.info("Account created with id: {}", savedAccount.getId());

        return mapToAccountResponse(savedAccount);
    }

    @Transactional
    public AccountResponse activateAccount(Long id) {
        log.info("Activating account id: {}", id);

        Account account = getAccountById(id);
        account.setStatus(AccountStatus.ACTIVE);
        Account savedAccount = accountRepository.save(account);

        return mapToAccountResponse(savedAccount);
    }

    @Transactional
    public AccountResponse deactivateAccount(Long id) {
        log.info("Deactivating account id: {}", id);

        Account account = getAccountById(id);
        account.setStatus(AccountStatus.LOCKED);
        Account savedAccount = accountRepository.save(account);

        return mapToAccountResponse(savedAccount);
    }

    // ─── HELPER METHODS ───────────────────────────────────────────────

    /**
     * Replaces the cashback account's id with {@code null} (and labels it
     * "CASHBACK") on a transaction so its real id is never exposed in history.
     * Runs inside a read-only transaction, so mutating the detached-for-write
     * fields is not flushed to the database.
     */
    private void maskCashbackAccount(TransactionLog txn) {
        if (txn.getFromAccountId() != null
                && txn.getFromAccountId() == RewardConstants.CASHBACK_ACCOUNT_ID) {
            txn.setFromAccountHolderName(RewardConstants.CASHBACK_ACCOUNT_NAME);
            txn.setFromAccountId(null);
        }
        if (txn.getToAccountId() != null
                && txn.getToAccountId() == RewardConstants.CASHBACK_ACCOUNT_ID) {
            txn.setToAccountHolderName(RewardConstants.CASHBACK_ACCOUNT_NAME);
            txn.setToAccountId(null);
        }
    }

    private AccountResponse mapToAccountResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .holderName(account.getHolderName())
                .balance(account.getBalance())
                .status(account.getStatus().name())
                .lastUpdated(account.getLastUpdated())
                .build();
    }

    public List<TransactionLog> getFilteredTransactionHistory(
            Long accountId,
            LocalDate startDate,
            LocalDate endDate) {

        log.debug("Fetching filtered transactions for account: {} from {} to {}",
                accountId, startDate, endDate);

        // Verify account exists
        getAccountById(accountId);

        // Convert LocalDate to LocalDateTime (start of day and end of day)
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // Get filtered transactions
        List<TransactionLog> transactions = transactionLogRepository
                .findByAccountIdAndDateRange(accountId, startDateTime, endDateTime);

        // Populate account holder names
        transactions.forEach(txn -> {
            if (txn.getFromAccountId() != null) {
                accountRepository.findById(txn.getFromAccountId())
                        .ifPresent(account ->
                                txn.setFromAccountHolderName(account.getHolderName())
                        );
            }

            if (txn.getToAccountId() != null) {
                accountRepository.findById(txn.getToAccountId())
                        .ifPresent(account ->
                                txn.setToAccountHolderName(account.getHolderName())
                        );
            }
        });

        // Hide the corporate cashback account's id from user-facing history;
        // it surfaces only as the "CASHBACK" label.
        transactions.forEach(this::maskCashbackAccount);

        return transactions;
    }

    /**
     * Get transactions for last week
     */
    public List<TransactionLog> getLastWeekTransactions(Long accountId) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusWeeks(1);
        return getFilteredTransactionHistory(accountId, startDate, endDate);
    }

    /**
     * Get transactions for last month
     */
    public List<TransactionLog> getLastMonthTransactions(Long accountId) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(1);
        return getFilteredTransactionHistory(accountId, startDate, endDate);
    }

    /**
     * Get transactions for last year
     */
    public List<TransactionLog> getLastYearTransactions(Long accountId) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusYears(1);
        return getFilteredTransactionHistory(accountId, startDate, endDate);
    }
}