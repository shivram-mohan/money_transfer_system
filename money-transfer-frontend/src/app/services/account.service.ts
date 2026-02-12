import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { AccountResponse } from '../models/account.model';
import { TransactionLog } from '../models/transaction.model';
 
@Injectable({
  providedIn: 'root'
})
export class AccountService {
  private apiUrl = environment.apiUrl;
 
  constructor(private http: HttpClient) {}
 
  getAccount(accountId: number): Observable<AccountResponse> {
    return this.http.get<AccountResponse>(`${this.apiUrl}/accounts/${accountId}`);
  }
 
  getBalance(accountId: number): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/accounts/${accountId}/balance`);
  }
 
  getTransactions(accountId: number): Observable<TransactionLog[]> {
    return this.http.get<TransactionLog[]>(`${this.apiUrl}/accounts/${accountId}/transactions`);
  }
}