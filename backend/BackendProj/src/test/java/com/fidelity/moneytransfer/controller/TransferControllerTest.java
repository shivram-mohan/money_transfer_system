package com.fidelity.moneytransfer.controller;

import com.fidelity.moneytransfer.dto.TransferRequest;
import com.fidelity.moneytransfer.dto.TransferResponse;
import com.fidelity.moneytransfer.service.TransferService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferControllerTest {

    @Mock private TransferService transferService;
    @InjectMocks private TransferController controller;

    @Test
    void transfer_DelegatesAndReturns200() {
        TransferRequest request = TransferRequest.builder()
                .fromAccountId(1L).toAccountId(2L)
                .amount(new BigDecimal("100.00")).idempotencyKey("k").build();
        TransferResponse expected = TransferResponse.builder()
                .status("SUCCESS").amount(new BigDecimal("100.00")).build();
        when(transferService.transfer(request)).thenReturn(expected);

        ResponseEntity<TransferResponse> response = controller.transfer(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(expected, response.getBody());
    }
}
