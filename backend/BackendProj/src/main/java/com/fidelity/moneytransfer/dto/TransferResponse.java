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

    private String TransactionId;
    private String status;
    private String message;
    private Long debitedFrom;
    private Long creditedTo;
    private BigDecimal amount;

    /** Rewards earned on this transfer (null/non-earning when no points apply). */
    private RewardEarnResult reward;
}
