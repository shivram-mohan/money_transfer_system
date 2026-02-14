// src/app/services/account.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { AccountResponse } from '../models/account.model';
import { TransactionLog } from '../models/transaction.model';
import { HttpParams } from '@angular/common/http';


export interface CreateAccountRequest {
  holderName: string;
  initialBalance: number;
}

@Injectable({
  providedIn: 'root'
})
export class AccountService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  // ─── USER METHODS ─────────────────────────────────────────────────

  getAccount(accountId: number): Observable<AccountResponse> {
    return this.http.get<AccountResponse>(
      `${this.apiUrl}/accounts/${accountId}`
    );
  }

  getBalance(accountId: number): Observable<number> {
    return this.http.get<number>(
      `${this.apiUrl}/accounts/${accountId}/balance`
    );
  }

  getTransactions(accountId: number): Observable<TransactionLog[]> {
    return this.http.get<TransactionLog[]>(
      `${this.apiUrl}/accounts/${accountId}/transactions`
    );
  }

  // ─── ADMIN METHODS ────────────────────────────────────────────────

  getAllAccounts(): Observable<AccountResponse[]> {
    return this.http.get<AccountResponse[]>(`${this.apiUrl}/accounts`);
  }

  createAccount(request: CreateAccountRequest): Observable<AccountResponse> {
    return this.http.post<AccountResponse>(`${this.apiUrl}/accounts`, request);
  }

  activateAccount(accountId: number): Observable<AccountResponse> {
    return this.http.put<AccountResponse>(
      `${this.apiUrl}/accounts/${accountId}/activate`, {}
    );
  }

  deactivateAccount(accountId: number): Observable<AccountResponse> {
    return this.http.put<AccountResponse>(
      `${this.apiUrl}/accounts/${accountId}/deactivate`, {}
    );
  }

  getFilteredTransactions(
  accountId: number,
  startDate: string,
  endDate: string
): Observable<TransactionLog[]> {
  const params = new HttpParams()
    .set('startDate', startDate)
    .set('endDate', endDate);
    
  return this.http.get<TransactionLog[]>(
    `${this.apiUrl}/accounts/${accountId}/transactions/filter`,
    { params }
  );
}

getLastWeekTransactions(accountId: number): Observable<TransactionLog[]> {
  return this.http.get<TransactionLog[]>(
    `${this.apiUrl}/accounts/${accountId}/transactions/last-week`
  );
}

getLastMonthTransactions(accountId: number): Observable<TransactionLog[]> {
  return this.http.get<TransactionLog[]>(
    `${this.apiUrl}/accounts/${accountId}/transactions/last-month`
  );
}

getLastYearTransactions(accountId: number): Observable<TransactionLog[]> {
  return this.http.get<TransactionLog[]>(
    `${this.apiUrl}/accounts/${accountId}/transactions/last-year`
  );
}

downloadPdfStatement(
  accountId: number,
  startDate?: string,
  endDate?: string
): Observable<Blob> {
  let params = new HttpParams();
  if (startDate) {
    params = params.set('startDate', startDate);
  }
  if (endDate) {
    params = params.set('endDate', endDate);
  }
  
  return this.http.get(
    `${this.apiUrl}/accounts/${accountId}/statement/pdf`,
    {
      params,
      responseType: 'blob' // Important for PDF download
    }
  );
}
}