package com.fidelity.moneytransfer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Login step 2: the user's password was already validated in step 1 (which is
 * what triggered the OTP email), so this step only needs the username and the
 * OTP. The password is intentionally NOT part of this payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginVerifyRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "OTP is required")
    private String otp;
}
