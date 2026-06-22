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
    // Plaintext balance: populated only for admin-facing responses.
    private BigDecimal balance;
    // AES-encrypted balance: populated for the logged-in user's own account so
    // the raw value never travels in the clear. Mutually exclusive with balance.
    private String encryptedBalance;
    private String status;
    private LocalDateTime lastUpdated;

}
