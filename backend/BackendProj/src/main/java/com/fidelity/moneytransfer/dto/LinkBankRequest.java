package com.fidelity.moneytransfer.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LinkBankRequest {

    @NotNull(message = "Account number is required")
    private Long accountNumber;
}
