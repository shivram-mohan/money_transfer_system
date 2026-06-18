//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.fidelity.moneytransfer.service;

import com.fidelity.moneytransfer.dto.RewardEarnResult;
import com.fidelity.moneytransfer.dto.TransferRequest;
import com.fidelity.moneytransfer.dto.TransferResponse;
import com.fidelity.moneytransfer.entity.Account;
import com.fidelity.moneytransfer.entity.TransactionLog;
import com.fidelity.moneytransfer.enums.TransactionStatus;
import com.fidelity.moneytransfer.exception.AccountNotActiveException;
import com.fidelity.moneytransfer.exception.DuplicateTransferException;
import com.fidelity.moneytransfer.exception.InsufficientBalanceException;
import com.fidelity.moneytransfer.repository.AccountRepository;
import com.fidelity.moneytransfer.repository.BankDetailsRepository;
import com.fidelity.moneytransfer.repository.TransactionLogRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {
    private static final Logger log = LoggerFactory.getLogger(TransferService.class);
    private final AccountService accountService;
    private final AccountRepository accountRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final BankDetailsRepository bankDetailsRepository;
    private final TransactionLogService transactionLogService;
    private final RewardService rewardService;

    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        log.info("Processing transfer: {} -> {}, amount: {}",
                request.getFromAccountId(),
                request.getToAccountId(),
                request.getAmount());

        try {
            // Step 1: Check for duplicate transfers
            checkIdempotency(request.getIdempotencyKey());

            // Step 2: Validate transfer request
            validateTransfer(request);

            // Step 3: Execute the transfer
            return executeTransfer(request);
        } catch (DuplicateTransferException e) {
            // Idempotent replay of an already-processed transfer is not a real
            // failure, so we don't add a FAILED row for it.
            throw e;
        } catch (Exception e) {
            // Record the failed attempt (insufficient balance, inactive account,
            // self-transfer, missing account, ...) in its own transaction so it
            // survives the rollback of this one, then re-throw for the API.
            log.warn("Transfer failed, recording failed transaction: {}", e.getMessage());
            transactionLogService.logFailedTransfer(request, e.getMessage());
            throw e;
        }
    }

    private void checkIdempotency(String idempotencyKey) {
        this.transactionLogRepository.findByIdempotencyKey(idempotencyKey).ifPresent((existingTransaction) -> {
            log.warn("Duplicate transfer detected with idempotency key: {}", idempotencyKey);
            throw new DuplicateTransferException("Transfer already processed with idempotency key: " + idempotencyKey);
        });
    }

    private void validateTransfer(TransferRequest request) {
        log.debug("Validating transfer: {}", request);
        // 1. Validate amount is positive
        if (request.getAmount() == null ||
                request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Transfer amount must be positive");
        }


        if (request.getFromAccountId().equals(request.getToAccountId())) {
            throw new IllegalArgumentException("Source and destination accounts must be different");
        } else {
            Account fromAccount = this.accountService.getAccountById(request.getFromAccountId());
            Account toAccount = this.accountService.getAccountById(request.getToAccountId());
            if (!fromAccount.isActive()) {
                throw new AccountNotActiveException("Source account is not active. Status: " + fromAccount.getStatus());
            } else if (!toAccount.isActive()) {
                throw new AccountNotActiveException("Destination account is not active. Status: " + toAccount.getStatus());
            } else if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
                BigDecimal var10002 = fromAccount.getBalance();
                throw new InsufficientBalanceException("Insufficient balance. Available: " + var10002 + ", Required: " + request.getAmount());
            }
        }
    }

    private TransferResponse executeTransfer(TransferRequest request) {
        log.info("Executing transfer from account {} to account {}, amount: {}", new Object[]{request.getFromAccountId(), request.getToAccountId(), request.getAmount()});

        Account fromAccount = this.accountService.getAccountById(request.getFromAccountId());
        Account toAccount = this.accountService.getAccountById(request.getToAccountId());
        fromAccount.debit(request.getAmount());
        toAccount.credit(request.getAmount());
        this.accountRepository.save(fromAccount);
        this.accountRepository.save(toAccount);
        // Keep the source-of-truth bank_details balances in sync with the accounts
        this.syncBankDetailsBalance(fromAccount);
        this.syncBankDetailsBalance(toAccount);
        // Failures here propagate to transfer(), which records the FAILED log in
        // a separate transaction so it isn't lost when this transaction rolls back.
        TransactionLog transactionLog = this.createTransactionLog(request, TransactionStatus.SUCCESS, (String) null);
        this.transactionLogRepository.save(transactionLog);
        log.info("Transfer successful. Transaction ID: {}", transactionLog.getId());

        // Award loyalty reward points to the sender. Runs in this same
        // transaction so points commit atomically with the money movement.
        RewardEarnResult reward = this.rewardService.awardForTransfer(
                request.getFromAccountId(), request.getToAccountId(), request.getAmount());

        return TransferResponse.builder().TransactionId(transactionLog.getId()).status("SUCCESS").message("Transfer completed successfully").debitedFrom(request.getFromAccountId()).creditedTo(request.getToAccountId()).amount(request.getAmount()).reward(reward).build();
    }

    /**
     * Mirrors an account's balance back to its originating bank_details record.
     * A linked account's id equals the bank account number, so we match on that.
     * Admin-created accounts have no bank_details row and are simply skipped.
     */
    private void syncBankDetailsBalance(Account account) {
        this.bankDetailsRepository.findByAccountNumber(account.getId())
                .ifPresent((bankDetails) -> {
                    bankDetails.setBalance(account.getBalance());
                    this.bankDetailsRepository.save(bankDetails);
                    log.debug("Synced bank_details balance for account {}: {}",
                            account.getId(), account.getBalance());
                });
    }

    private TransactionLog createTransactionLog(
            TransferRequest request,
            TransactionStatus status,  // ← Enum parameter
            String failureReason) {

        return TransactionLog.builder()
                // ID will be auto-generated by @PrePersist
                .fromAccountId(request.getFromAccountId())
                .toAccountId(request.getToAccountId())
                .amount(request.getAmount())
                .status(status)  // ← Use enum directly
                .failureReason(failureReason)
                .idempotencyKey(request.getIdempotencyKey())
                // createdOn will be auto-set by @PrePersist
                .build();
    }

    public TransferService(final AccountService accountService, final AccountRepository accountRepository, final TransactionLogRepository transactionLogRepository, final BankDetailsRepository bankDetailsRepository, final TransactionLogService transactionLogService, final RewardService rewardService) {
        this.accountService = accountService;
        this.accountRepository = accountRepository;
        this.transactionLogRepository = transactionLogRepository;
        this.bankDetailsRepository = bankDetailsRepository;
        this.transactionLogService = transactionLogService;
        this.rewardService = rewardService;
    }
}