package com.fidelity.moneytransfer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LinkBankResponse {
    private Long accountId;
    private String holderName;
    // AES-encrypted balance of the freshly linked account (see CryptoService);
    // the client decrypts it only when the user reveals their balance.
    private String encryptedBalance;
    private String message;
}
