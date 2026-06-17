package com.fidelity.moneytransfer.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SHA-256 helper that produces the exact same lowercase-hex digest as the
 * browser's Web Crypto API ({@code crypto.subtle.digest('SHA-256', ...)}).
 *
 * The frontend hashes the user's password with SHA-256 before sending it over
 * the wire (so the raw password is never visible in the network inspector).
 * The backend then bcrypt-encodes that hash for storage. This util lets the
 * seeder reproduce the same client-side hash for the bootstrap admin account.
 */
public final class PasswordHasher {

    private PasswordHasher() {
    }

    public static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
