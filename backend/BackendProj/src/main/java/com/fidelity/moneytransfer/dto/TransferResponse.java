package com.fidelity.moneytransfer.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransferResponse {

    private String transactionId;
    private String status;
    private String message;
    private Long debitedFrom;
    private Long creditedTo;
    private BigDecimal amount;
}
