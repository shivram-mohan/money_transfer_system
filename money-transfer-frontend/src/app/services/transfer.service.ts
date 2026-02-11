// src/app/services/transfer.service.ts

import { Injectable } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { delay } from 'rxjs/operators';
import { TransferRequest, TransferResponse, TransactionLog, TransactionStatus } from '../models/transaction.model';
import { AccountService } from './account.service';

@Injectable({
  providedIn: 'root'
})
export class TransferService {
  private usedIdempotencyKeys: Set<string> = new Set();

  constructor(private accountService: AccountService) {}

  transfer(request: TransferRequest): Observable<TransferResponse> {
    return new Observable(observer => {
      setTimeout(() => {
        // Validate idempotency
        if (this.usedIdempotencyKeys.has(request.idempotencyKey)) {
          observer.error({
            errorCode: 'TRX-409',
            message: 'Duplicate transfer request'
          });
          return;
        }

        // Validate accounts are different
        if (request.fromAccountId === request.toAccountId) {
          observer.error({
            errorCode: 'VAL-422',
            message: 'Source and destination accounts must be different'
          });
          return;
        }

        // Validate amount
        if (request.amount <= 0) {
          observer.error({
            errorCode: 'VAL-422',
            message: 'Amount must be greater than 0'
          });
          return;
        }

        // Get account details and validate
        this.accountService.getAccount(request.fromAccountId).subscribe({
          next: (fromAccount) => {
            // Check if account is active
            if (fromAccount.status !== 'ACTIVE') {
              observer.error({
                errorCode: 'ACC-403',
                message: 'Source account is not active'
              });
              return;
            }

            // Check sufficient balance
            if (fromAccount.balance < request.amount) {
              observer.error({
                errorCode: 'TRX-400',
                message: 'Insufficient funds in source account'
              });
              return;
            }

            // Validate destination account
            this.accountService.getAccount(request.toAccountId).subscribe({
              next: (toAccount) => {
                if (toAccount.status !== 'ACTIVE') {
                  observer.error({
                    errorCode: 'ACC-403',
                    message: 'Destination account is not active'
                  });
                  return;
                }

                // Perform transfer
                const newFromBalance = fromAccount.balance - request.amount;
                const newToBalance = toAccount.balance + request.amount;

                // Update balances
                this.accountService.updateBalance(request.fromAccountId, newFromBalance);
                this.accountService.updateBalance(request.toAccountId, newToBalance);

                // Create transaction log
                const transactionLog: TransactionLog = {
                  id: `TRX-${Date.now()}`,
                  fromAccountId: request.fromAccountId,
                  toAccountId: request.toAccountId,
                  amount: request.amount,
                  status: TransactionStatus.SUCCESS,
                  idempotencyKey: request.idempotencyKey,
                  createdOn: new Date()
                };

                this.accountService.addTransaction(transactionLog);
                this.usedIdempotencyKeys.add(request.idempotencyKey);

                // Create response
                const response: TransferResponse = {
                  transactionId: transactionLog.id,
                  status: 'SUCCESS',
                  message: 'Transfer completed successfully',
                  debitedFrom: request.fromAccountId,
                  creditedTo: request.toAccountId,
                  amount: request.amount
                };

                observer.next(response);
                observer.complete();
              },
              error: (err) => {
                observer.error({
                  errorCode: 'ACC-404',
                  message: 'Destination account not found'
                });
              }
            });
          },
          error: (err) => {
            observer.error({
              errorCode: 'ACC-404',
              message: 'Source account not found'
            });
          }
        });
      }, 1000); // Simulate network delay
    });
  }

  // Generate unique idempotency key
  generateIdempotencyKey(): string {
    return `${Date.now()}-${Math.random().toString(36).substring(2, 15)}`;
  }
}