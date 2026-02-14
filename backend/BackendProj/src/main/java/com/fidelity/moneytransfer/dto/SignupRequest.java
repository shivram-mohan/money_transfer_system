package com.fidelity.moneytransfer.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignupRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Name is required")
    @Size(min = 2, message = "Name must be at least 2 characters")
    private String name;

    @Email(message = "Invalid email format")
    private String email;

    @NotNull(message = "Initial balance is required")
    @DecimalMin(value = "100.0", message = "Minimum initial balance is ₹100")
    @DecimalMax(value = "1000000.0", message = "Maximum initial balance is ₹10,00,000")
    private BigDecimal initialBalance;
}