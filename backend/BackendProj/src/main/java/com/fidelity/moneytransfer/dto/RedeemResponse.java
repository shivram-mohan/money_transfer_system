package com.fidelity.moneytransfer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Result of a successful cash redemption. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedeemResponse {

    private long redeemedPoints;
    private BigDecimal amountCredited;
    private long remainingPoints;
    private String transactionId;
    private String message;
}
