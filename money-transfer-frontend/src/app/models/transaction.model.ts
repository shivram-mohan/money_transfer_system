// src/app/models/transaction.model.ts

export enum TransactionStatus {
  SUCCESS = 'SUCCESS',
  FAILED = 'FAILED'
}

export interface TransactionLog {
  id: string;
  fromAccountId: number;
  toAccountId: number;
  amount: number;
  status: TransactionStatus;
  failureReason?: string;
  idempotencyKey: string;
  createdOn: Date;
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
}

export interface ErrorResponse {
  errorCode: string;
  message: string;
}