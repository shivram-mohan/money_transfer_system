//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.fidelity.moneytransfer.service;

import com.fidelity.moneytransfer.dto.TransferRequest;
import com.fidelity.moneytransfer.dto.TransferResponse;
import com.fidelity.moneytransfer.entity.Account;
import com.fidelity.moneytransfer.entity.TransactionLog;
import com.fidelity.moneytransfer.enums.TransactionStatus;
import com.fidelity.moneytransfer.exception.AccountNotActiveException;
import com.fidelity.moneytransfer.exception.DuplicateTransferException;
import com.fidelity.moneytransfer.exception.InsufficientBalanceException;
import com.fidelity.moneytransfer.repository.AccountRepository;
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

    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        log.info("Processing transfer request: {}", request);
        this.checkIdempotency(request.getIdempotencyKey());
        this.validateTransfer(request);
        return this.executeTransfer(request);
    }

    private void checkIdempotency(String idempotencyKey) {
        this.transactionLogRepository.findByIdempotencyKey(idempotencyKey).ifPresent((existingTransaction) -> {
            log.warn("Duplicate transfer detected with idempotency key: {}", idempotencyKey);
            throw new DuplicateTransferException("Transfer already processed with idempotency key: " + idempotencyKey);
        });
    }

    private void validateTransfer(TransferRequest request) {
        log.debug("Validating transfer: {}", request);
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
        TransactionLog transactionLog = null;

        try {
            Account fromAccount = this.accountService.getAccountById(request.getFromAccountId());
            Account toAccount = this.accountService.getAccountById(request.getToAccountId());
            fromAccount.debit(request.getAmount());
            toAccount.credit(request.getAmount());
            this.accountRepository.save(fromAccount);
            this.accountRepository.save(toAccount);
            transactionLog = this.createTransactionLog(request, TransactionStatus.SUCCESS, (String)null);
            this.transactionLogRepository.save(transactionLog);
            log.info("Transfer successful. Transaction ID: {}", transactionLog.getId());
            return TransferResponse.builder().transactionId(transactionLog.getId()).status("SUCCESS").message("Transfer completed successfully").debitedFrom(request.getFromAccountId()).creditedTo(request.getToAccountId()).amount(request.getAmount()).build();
        } catch (Exception var5) {
            log.error("Transfer failed: {}", var5.getMessage(), var5);
            transactionLog = this.createTransactionLog(request, TransactionStatus.FAILED, var5.getMessage());
            this.transactionLogRepository.save(transactionLog);
            throw var5;
        }
    }

    private TransactionLog createTransactionLog(TransferRequest request, TransactionStatus status, String failureReason) {
        return TransactionLog.builder().id(UUID.randomUUID().toString()).fromAccountId(request.getFromAccountId()).toAccountId(request.getToAccountId()).amount(request.getAmount()).status(status).failureReason(failureReason).idempotencyKey(request.getIdempotencyKey()).build();
    }

    public TransferService(final AccountService accountService, final AccountRepository accountRepository, final TransactionLogRepository transactionLogRepository) {
        this.accountService = accountService;
        this.accountRepository = accountRepository;
        this.transactionLogRepository = transactionLogRepository;
    }
}
