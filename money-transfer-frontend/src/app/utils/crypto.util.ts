// src/app/utils/crypto.util.ts

/**
 * Hash a password with SHA-256 and return a lowercase hex string.
 *
 * The hash is sent to the backend instead of the raw password, so the
 * plaintext password is never visible in the browser's network inspector.
 * The backend bcrypt-encodes this hash for storage. The output matches Java's
 * MessageDigest "SHA-256" hex, so the same value can be reproduced server-side.
 */
export async function hashPassword(password: string): Promise<string> {
  const data = new TextEncoder().encode(password);
  const digest = await crypto.subtle.digest('SHA-256', data);
  return Array.from(new Uint8Array(digest))
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('');
}
