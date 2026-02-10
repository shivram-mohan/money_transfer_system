// src/app/services/auth.service.ts

import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, of, throwError } from 'rxjs';
import { delay } from 'rxjs/operators';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  accountId: number;
  holderName: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly TOKEN_KEY = 'auth_token';
  private readonly ACCOUNT_ID_KEY = 'account_id';
  private readonly HOLDER_NAME_KEY = 'holder_name';
  
  private isAuthenticatedSubject = new BehaviorSubject<boolean>(this.hasToken());
  public isAuthenticated$ = this.isAuthenticatedSubject.asObservable();

  // Hardcoded user credentials for testing
  private mockUsers = [
    { username: 'john.doe', password: 'password123', accountId: 1, holderName: 'John Doe', token: 'mock-token-john' },
    { username: 'jane.smith', password: 'password123', accountId: 2, holderName: 'Jane Smith', token: 'mock-token-jane' },
    { username: 'demo', password: 'demo', accountId: 1, holderName: 'Demo User', token: 'mock-token-demo' }
  ];

  constructor() {}

  login(credentials: LoginRequest): Observable<LoginResponse> {
    // Simulate API call with delay
    return new Observable(observer => {
      setTimeout(() => {
        const user = this.mockUsers.find(
          u => u.username === credentials.username && u.password === credentials.password
        );

        if (user) {
          const response: LoginResponse = {
            token: user.token,
            accountId: user.accountId,
            holderName: user.holderName
          };
          
          // Store in localStorage
          localStorage.setItem(this.TOKEN_KEY, response.token);
          localStorage.setItem(this.ACCOUNT_ID_KEY, response.accountId.toString());
          localStorage.setItem(this.HOLDER_NAME_KEY, response.holderName);
          
          this.isAuthenticatedSubject.next(true);
          observer.next(response);
          observer.complete();
        } else {
          observer.error({ message: 'Invalid username or password' });
        }
      }, 800); // Simulate network delay
    });
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.ACCOUNT_ID_KEY);
    localStorage.removeItem(this.HOLDER_NAME_KEY);
    this.isAuthenticatedSubject.next(false);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  getCurrentAccountId(): number | null {
    const accountId = localStorage.getItem(this.ACCOUNT_ID_KEY);
    return accountId ? parseInt(accountId, 10) : null;
  }

  getHolderName(): string | null {
    return localStorage.getItem(this.HOLDER_NAME_KEY);
  }

  isAuthenticated(): boolean {
    return this.hasToken();
  }

  private hasToken(): boolean {
    return !!this.getToken();
  }
}