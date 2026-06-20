package com.fidelity.moneytransfer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "OTP is required")
    private String otp;

    // Plaintext new password (sent over HTTPS); bcrypt-encoded on save
    @NotBlank(message = "Password is required")
    private String password;
}
