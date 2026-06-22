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
    @InjectMocks private TransactionLogService service;

    @Test
    void logFailedTransfer_PersistsFailedRow_WithoutIdempotencyKey() {
        TransferRequest request = TransferRequest.builder()
                .fromAccountId(1L).toAccountId(2L)
                .amount(new BigDecimal("250.00")).idempotencyKey("key").build();

        service.logFailedTransfer(request, "Insufficient balance");

        ArgumentCaptor<TransactionLog> captor =
                ArgumentCaptor.forClass(TransactionLog.class);
        verify(transactionLogRepository).save(captor.capture());
        TransactionLog log = captor.getValue();

        assertEquals(TransactionStatus.FAILED, log.getStatus());
        assertEquals("Insufficient balance", log.getFailureReason());
        assertEquals(new BigDecimal("250.00"), log.getAmount());
        assertNull(log.getIdempotencyKey());
    }

    @Test
    void logFailedTransfer_NullAmount_DefaultsToZero() {
        TransferRequest request = TransferRequest.builder()
                .fromAccountId(1L).toAccountId(2L)
                .amount(null).idempotencyKey("key").build();

        service.logFailedTransfer(request, "Malformed");

        ArgumentCaptor<TransactionLog> captor =
                ArgumentCaptor.forClass(TransactionLog.class);
        verify(transactionLogRepository).save(captor.capture());
        assertEquals(BigDecimal.ZERO, captor.getValue().getAmount());
    }
}
