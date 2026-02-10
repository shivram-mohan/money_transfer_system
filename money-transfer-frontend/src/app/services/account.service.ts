// src/app/services/account.service.ts

import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';
import { Account, AccountResponse, BalanceResponse, AccountStatus } from '../models/account.model';
import { TransactionLog, TransactionStatus } from '../models/transaction.model';

@Injectable({
  providedIn: 'root'
})
export class AccountService {
  // Hardcoded mock data
  private mockAccounts: Account[] = [
    {
      id: 1,
      holderName: 'John Doe',
      balance: 15000.00,
      status: AccountStatus.ACTIVE,
      version: 1,
      lastUpdated: new Date('2024-02-08T10:30:00')
    },
    {
      id: 2,
      holderName: 'Jane Smith',
      balance: 25000.00,
      status: AccountStatus.ACTIVE,
      version: 1,
      lastUpdated: new Date('2024-02-08T09:15:00')
    },
    {
      id: 3,
      holderName: 'Bob Johnson',
      balance: 8500.00,
      status: AccountStatus.ACTIVE,
      version: 1,
      lastUpdated: new Date('2024-02-07T14:20:00')
    },
    {
      id: 4,
      holderName: 'Alice Williams',
      balance: 50000.00,
      status: AccountStatus.ACTIVE,
      version: 1,
      lastUpdated: new Date('2024-02-08T11:45:00')
    },
    {
      id: 5,
      holderName: 'Charlie Brown',
      balance: 3200.00,
      status: AccountStatus.LOCKED,
      version: 1,
      lastUpdated: new Date('2024-02-06T16:00:00')
    }
  ];

  private mockTransactions: TransactionLog[] = [
    {
      id: 'TRX-001',
      fromAccountId: 1,
      toAccountId: 2,
      amount: 500.00,
      status: TransactionStatus.SUCCESS,
      idempotencyKey: 'key-001',
      createdOn: new Date('2024-02-08T09:30:00')
    },
    {
      id: 'TRX-002',
      fromAccountId: 2,
      toAccountId: 1,
      amount: 1000.00,
      status: TransactionStatus.SUCCESS,
      idempotencyKey: 'key-002',
      createdOn: new Date('2024-02-08T10:15:00')
    },
    {
      id: 'TRX-003',
      fromAccountId: 1,
      toAccountId: 3,
      amount: 250.00,
      status: TransactionStatus.SUCCESS,
      idempotencyKey: 'key-003',
      createdOn: new Date('2024-02-08T11:00:00')
    },
    {
      id: 'TRX-004',
      fromAccountId: 4,
      toAccountId: 1,
      amount: 2000.00,
      status: TransactionStatus.SUCCESS,
      idempotencyKey: 'key-004',
      createdOn: new Date('2024-02-08T12:30:00')
    },
    {
      id: 'TRX-005',
      fromAccountId: 1,
      toAccountId: 5,
      amount: 5000.00,
      status: TransactionStatus.FAILED,
      failureReason: 'Destination account is locked',
      idempotencyKey: 'key-005',
      createdOn: new Date('2024-02-08T13:45:00')
    }
  ];

  constructor() {}

  getAccount(accountId: number): Observable<AccountResponse> {
    const account = this.mockAccounts.find(acc => acc.id === accountId);
    
    if (account) {
      const response: AccountResponse = {
        id: account.id,
        holderName: account.holderName,
        balance: account.balance,
        status: account.status
      };
      return of(response).pipe(delay(500));
    }
    
    throw new Error('Account not found');
  }

  getBalance(accountId: number): Observable<BalanceResponse> {
    const account = this.mockAccounts.find(acc => acc.id === accountId);
    
    if (account) {
      const response: BalanceResponse = {
        accountId: account.id,
        balance: account.balance
      };
      return of(response).pipe(delay(300));
    }
    
    throw new Error('Account not found');
  }

  getTransactions(accountId: number): Observable<TransactionLog[]> {
    // Filter transactions where account is either sender or receiver
    const transactions = this.mockTransactions.filter(
      txn => txn.fromAccountId === accountId || txn.toAccountId === accountId
    ).sort((a, b) => b.createdOn.getTime() - a.createdOn.getTime()); // Sort by date descending
    
    return of(transactions).pipe(delay(500));
  }

  // Update balance after transfer (for mock purposes)
  updateBalance(accountId: number, newBalance: number): void {
    const account = this.mockAccounts.find(acc => acc.id === accountId);
    if (account) {
      account.balance = newBalance;
      account.lastUpdated = new Date();
    }
  }

  // Add transaction to mock data
  addTransaction(transaction: TransactionLog): void {
    this.mockTransactions.unshift(transaction); // Add to beginning
  }
}