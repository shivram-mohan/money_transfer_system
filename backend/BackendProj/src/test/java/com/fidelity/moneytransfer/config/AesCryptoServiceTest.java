package com.fidelity.moneytransfer.config;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class AesCryptoServiceTest {

    private final AesCryptoService crypto = new AesCryptoService("test-secret-123");

    @Test
    void encryptThenDecrypt_roundTrips() {
        String cipher = crypto.encrypt("1234.56");
        assertNotEquals("1234.56", cipher);            // not plaintext
        assertEquals("1234.56", crypto.decrypt(cipher)); // reversible
    }

    @Test
    void encrypt_usesRandomIv_soOutputDiffersEachCall() {
        String a = crypto.encrypt("1000.00");
        String b = crypto.encrypt("1000.00");
        assertNotEquals(a, b, "random IV should make ciphertexts differ");
        assertEquals("1000.00", crypto.decrypt(a));
        assertEquals("1000.00", crypto.decrypt(b));
    }

    @Test
    void decrypt_withMatchingKey_acrossInstances() {
        AesCryptoService other = new AesCryptoService("test-secret-123");
        assertEquals("42.00", other.decrypt(crypto.encrypt("42.00")));
    }

    @Test
    void decrypt_invalidBase64_throws() {
        // '!' is not a Base64 character; decoding fails before any cipher work.
        assertThrows(RuntimeException.class, () -> crypto.decrypt("not-valid-base64!!"));
    }

    @Test
    void decrypt_malformedCipherLength_wrapsAsIllegalState() {
        // 20 bytes => 16-byte IV + 4-byte body; 4 is not a valid AES block length,
        // so the cipher throws a GeneralSecurityException that is wrapped.
        String malformed = Base64.getEncoder().encodeToString(new byte[20]);
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> crypto.decrypt(malformed));
        assertEquals("Failed to decrypt value", ex.getMessage());
    }
}
