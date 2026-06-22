// src/app/services/crypto.service.ts

import { Injectable } from '@angular/core';
import { environment } from '../../environments/environment';

/**
 * Decrypts values the backend AES-encrypts (e.g. the account balance) so they
 * never travel in the clear and can't be read straight from the network tab.
 *
 * Mirrors the backend (AesCryptoService): the key is SHA-256(secret); the
 * payload is Base64(IV(16 bytes) || ciphertext), AES/CBC/PKCS7. Uses the
 * built-in Web Crypto API, so there is no third-party crypto dependency.
 */
@Injectable({ providedIn: 'root' })
export class CryptoService {
  private readonly secret = environment.balanceSecret;

  /** Decrypts a Base64 IV||ciphertext blob back to its plaintext string. */
  async decrypt(cipherBase64: string): Promise<string> {
    const data = this.base64ToBytes(cipherBase64);
    const iv = data.slice(0, 16);
    const ciphertext = data.slice(16);

    const keyMaterial = await crypto.subtle.digest(
      'SHA-256',
      new TextEncoder().encode(this.secret)
    );
    const key = await crypto.subtle.importKey(
      'raw',
      keyMaterial,
      { name: 'AES-CBC' },
      false,
      ['decrypt']
    );

    const plainBuffer = await crypto.subtle.decrypt(
      { name: 'AES-CBC', iv },
      key,
      ciphertext
    );
    return new TextDecoder().decode(plainBuffer);
  }

  /** Convenience wrapper that parses the decrypted plaintext as a number. */
  async decryptToNumber(cipherBase64: string): Promise<number> {
    return parseFloat(await this.decrypt(cipherBase64));
  }

  private base64ToBytes(base64: string): Uint8Array {
    const binary = atob(base64);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) {
      bytes[i] = binary.charCodeAt(i);
    }
    return bytes;
  }
}
