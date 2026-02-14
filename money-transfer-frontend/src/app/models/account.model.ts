// src/app/models/account.model.ts

export enum AccountStatus {
  ACTIVE = 'ACTIVE',
  LOCKED = 'LOCKED',
  CLOSED = 'CLOSED'
}
export enum AccountType {
  SAVINGS = 'SAVINGS',
  CURRENT = 'CURRENT'
}
export interface Account {
  id: number;
  holderName: string;
  balance: number;
  status: AccountStatus;
  version: number;
  lastUpdated: Date;
  accountType: AccountType; // ✅ NEW
  monthlyTransactionCount?: number; // ✅ NEW
  dailyWithdrawalAmount?: number; // ✅ NEW
}

export interface AccountResponse {
  id: number;
  holderName: string;
  balance: number;
  status: string;
  accountType: string;
  monthlyTransactionCount?: number; // ✅ ADD THIS
  dailyWithdrawalAmount?: number;
}

export interface BalanceResponse {
  accountId: number;
  balance: number;
}

export interface CreateAccountRequest {
  holderName: string;
  initialBalance: number;
  accountType: AccountType; // ✅ NEW
}