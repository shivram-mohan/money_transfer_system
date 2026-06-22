export const environment = {
    production: false,
    apiUrl: 'http://localhost:8080/api/v1',
    // Shared secret used to AES-decrypt the balance returned by the backend so it
    // is not readable in plaintext in the network tab. Must match the backend's
    // `app.crypto.secret`.
    balanceSecret: 'b4l4nc3-3ncrypt10n-s3cr3t-ch4ng3-m3-1n-pr0d'
  };


