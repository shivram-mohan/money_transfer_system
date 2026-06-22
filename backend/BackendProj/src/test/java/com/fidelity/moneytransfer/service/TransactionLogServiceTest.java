package com.fidelity.moneytransfer.service;

import com.fidelity.moneytransfer.dto.TransferRequest;
import com.fidelity.moneytransfer.entity.TransactionLog;
import com.fidelity.moneytransfer.enums.TransactionStatus;
import com.fidelity.moneytransfer.repository.TransactionLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionLogServiceTest {

    @Mock private TransactionLogRepository transactionLogRepository;
    @InjectMocks private TransactionLogService transactionLogService;

    @Test
    void logFailedTransfer_withAmount_savesFailedLog() {
        TransferRequest request = TransferRequest.builder()
                .fromAccountId(1L).toAccountId(2L)
                .amount(new BigDecimal("50.00")).idempotencyKey("key").build();

        transactionLogService.logFailedTransfer(request, "Insufficient balance");

        ArgumentCaptor<TransactionLog> captor = ArgumentCaptor.forClass(TransactionLog.class);
        verify(transactionLogRepository).save(captor.capture());
        TransactionLog saved = captor.getValue();
        assertEquals(TransactionStatus.FAILED, saved.getStatus());
        assertEquals(new BigDecimal("50.00"), saved.getAmount());
        assertEquals("Insufficient balance", saved.getFailureReason());
        assertNull(saved.getIdempotencyKey()); // never stored on a failed log
    }

    @Test
    void logFailedTransfer_nullAmount_defaultsToZero() {
        TransferRequest request = TransferRequest.builder()
                .fromAccountId(1L).toAccountId(2L).amount(null).idempotencyKey("key").build();

        transactionLogService.logFailedTransfer(request, "Bad request");

        ArgumentCaptor<TransactionLog> captor = ArgumentCaptor.forClass(TransactionLog.class);
        verify(transactionLogRepository).save(captor.capture());
        assertEquals(BigDecimal.ZERO, captor.getValue().getAmount());
    }
}
