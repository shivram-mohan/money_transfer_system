package com.fidelity.moneytransfer.dto;

import com.fidelity.moneytransfer.enums.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAccountRequest {

    @NotBlank(message = "Holder name is required")
    @Size(min = 2, max = 100, message = "Holder name must be 2-100 characters")
    private String holderName;

    @NotNull(message = "Initial balance is required")
    @DecimalMin(value = "0.0", message = "Initial balance must be positive")
    private BigDecimal initialBalance;

    // ✅ NEW - Account Type
    @NotNull(message = "Account type is required")
    private AccountType accountType;
}