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

    /** Reward points outcome for this transfer (null if not applicable). */
    private RewardResult reward;
}
