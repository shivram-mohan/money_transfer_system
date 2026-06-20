package com.fidelity.moneytransfer.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request to redeem reward points as cash (1 point = 1 currency). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RedeemRequest {

    @NotNull(message = "Points to redeem is required")
    @Min(value = 1, message = "Points to redeem must be positive")
    private Long points;
}
