package com.fidelity.moneytransfer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Signup step 1: the user picks a username and email. We verify the email is
 * theirs by sending an OTP. Bank account linking happens later, after login.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerifyAccountRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
}
