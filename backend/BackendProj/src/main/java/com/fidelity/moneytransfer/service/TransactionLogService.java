package com.fidelity.moneytransfer.service;

import com.fidelity.moneytransfer.dto.TransferRequest;
import com.fidelity.moneytransfer.entity.TransactionLog;
import com.fidelity.moneytransfer.enums.TransactionStatus;
import com.fidelity.moneytransfer.repository.TransactionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Persists FAILED transaction records.
 * <p>
 * A failed transfer (e.g. insufficient balance) rolls back the main transfer
 * transaction. If we logged the failure inside that same transaction the log
 * row would be rolled back too. This service therefore runs in its OWN
 * transaction ({@link Propagation#REQUIRES_NEW}) so the failure record survives
 * the rollback of the surrounding transfer. It lives in a separate bean so the
 * REQUIRES_NEW propagation is honoured (self-invocation would bypass the proxy).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionLogService {

    private final TransactionLogRepository transactionLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailedTransfer(TransferRequest request, String failureReason) {
        TransactionLog failedLog = TransactionLog.builder()
                .fromAccountId(request.getFromAccountId())
                .toAccountId(request.getToAccountId())
                // Amount may be missing on a malformed request; default to zero
                // so the (non-null) column constraint is still satisfied.
                .amount(request.getAmount() != null ? request.getAmount() : BigDecimal.ZERO)
                .status(TransactionStatus.FAILED)
                .failureReason(failureReason)
                // Deliberately NOT storing the idempotency key on a failed log:
                // the unique constraint would otherwise block the user from
                // retrying the same transfer after fixing the problem.
                .idempotencyKey(null)
                .build();

        transactionLogRepository.save(failedLog);
        log.info("Logged FAILED transfer {} -> {} (reason: {})",
                request.getFromAccountId(), request.getToAccountId(), failureReason);
    }
}
