// src/app/models/transaction.model.ts

import { RewardResult } from './reward.model';

export enum TransactionStatus {
  SUCCESS = 'SUCCESS',
  FAILED = 'FAILED'
}

export interface TransactionLog {
  id: string;
  // null when the counterparty is the masked CASHBACK account
  fromAccountId: number | null;
  toAccountId: number | null;
  amount: number;
  status: TransactionStatus;
  failureReason?: string;
  idempotencyKey: string;
  createdOn: Date;
   fromAccountHolderName?: string;
  toAccountHolderName?: string
}

export interface TransferRequest {
  fromAccountId: number;
  toAccountId: number;
  amount: number;
  idempotencyKey: string;
}

export interface TransferResponse {
  transactionId: string;
  status: string;
  message: string;
  debitedFrom: number;
  creditedTo: number;
  amount: number;
  reward?: RewardResult | null;
}

export interface ErrorResponse {
  errorCode: string;
  message: string;
}