package com.fidelity.moneytransfer.config;

/**
 * Abstraction for symmetric encryption of values that must not be readable in
 * transit by a casual observer (e.g. a user inspecting their own browser's
 * network tab). Depending on an interface keeps callers decoupled from the
 * concrete cipher (Dependency Inversion) and makes them trivially testable.
 */
public interface CryptoService {

    /** Encrypts plaintext, returning an opaque, transport-safe string. */
    String encrypt(String plaintext);

    /** Reverses {@link #encrypt(String)}. */
    String decrypt(String ciphertext);
}
