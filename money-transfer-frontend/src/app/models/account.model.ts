// src/app/models/account.model.ts

export enum AccountStatus {
  ACTIVE = 'ACTIVE',
  LOCKED = 'LOCKED',
  CLOSED = 'CLOSED'
}

export interface Account {
  id: number;
  holderName: string;
  balance: number;
  status: AccountStatus;
  version: number;
  lastUpdated: Date;
}

export interface AccountResponse {
  id: number;
  holderName: string;
  // Plaintext balance is only present on admin responses. For the logged-in
  // user's own account the backend sends `encryptedBalance` instead.
  balance?: number;
  encryptedBalance?: string;
  status: string;
}

export interface BalanceResponse {
  accountId: number;
  balance: number;
}