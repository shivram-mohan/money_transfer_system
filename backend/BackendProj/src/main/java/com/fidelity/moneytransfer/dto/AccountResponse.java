package com.fidelity.moneytransfer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class AccountResponse {
    private Long id;
    private String holderName;
    private BigDecimal balance;
    private String status;
    private LocalDateTime lastUpdated;
    private String accountType;
}
