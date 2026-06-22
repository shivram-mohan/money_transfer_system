package com.fidelity.moneytransfer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fidelity.moneytransfer.config.GlobalExceptionHandler;
import com.fidelity.moneytransfer.dto.TransferRequest;
import com.fidelity.moneytransfer.dto.TransferResponse;
import com.fidelity.moneytransfer.exception.InsufficientBalanceException;
import com.fidelity.moneytransfer.service.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TransferControllerTest {

    @Mock private TransferService transferService;
    @InjectMocks private TransferController controller;

    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private TransferRequest validRequest() {
        return TransferRequest.builder().fromAccountId(1L).toAccountId(2L)
                .amount(new BigDecimal("100.00")).idempotencyKey("k1").build();
    }

    @Test
    void transfer_success() throws Exception {
        TransferResponse resp = TransferResponse.builder()
                .TransactionId("tx").status("SUCCESS").message("ok")
                .debitedFrom(1L).creditedTo(2L).amount(new BigDecimal("100.00")).build();
        when(transferService.transfer(any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void transfer_insufficientBalance_400() throws Exception {
        when(transferService.transfer(any()))
                .thenThrow(new InsufficientBalanceException("Insufficient balance"));
        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(validRequest())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transfer_validationError_422() throws Exception {
        // Missing amount / negative account id triggers bean validation.
        TransferRequest bad = TransferRequest.builder()
                .fromAccountId(-1L).toAccountId(2L).idempotencyKey("k").build();
        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(bad)))
                .andExpect(status().isUnprocessableEntity());
    }
}
