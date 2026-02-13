// src/app/services/auth.service.ts

import { Injectable, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { UserRole } from '../models/user.model';
import { environment } from '../../environments/environment';

export interface LoginRequest {
  username: string;
  password: string;
  isAdmin?: boolean;
}

export interface LoginResponse {
  token: string;
  accountId: number;
  holderName: string;
  userId: number;
  role: UserRole;
  username: string;
  expiresIn: number;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly TOKEN_KEY = 'auth_token';
  private readonly ACCOUNT_ID_KEY = 'account_id';
  private readonly HOLDER_NAME_KEY = 'holder_name';
  private readonly USER_ID_KEY = 'user_id';
  private readonly USER_ROLE_KEY = 'user_role';
  private readonly USERNAME_KEY = 'username';

  private platformId = inject(PLATFORM_ID);
  private isBrowser: boolean;

  private isAuthenticatedSubject: BehaviorSubject<boolean>;
  public isAuthenticated$: Observable<boolean>;

  constructor(private http: HttpClient) {
    this.isBrowser = isPlatformBrowser(this.platformId);
    this.isAuthenticatedSubject = new BehaviorSubject<boolean>(
      this.hasToken()
    );
    this.isAuthenticated$ = this.isAuthenticatedSubject.asObservable();
  }

  login(credentials: LoginRequest): Observable<LoginResponse> {
    // Call the new JWT login endpoint
    return this.http.post<LoginResponse>(
      `${environment.apiUrl}/auth/login`,
      {
        username: credentials.username,
        password: credentials.password
      }
    ).pipe(
      tap((response: LoginResponse) => {
        if (this.isBrowser) {
          // Store JWT token and user info
          localStorage.setItem(this.TOKEN_KEY, response.token);
          localStorage.setItem(
            this.ACCOUNT_ID_KEY, 
            response.accountId?.toString() || '0'
          );
          localStorage.setItem(
            this.HOLDER_NAME_KEY, 
            response.holderName
          );
          localStorage.setItem(this.USER_ID_KEY, '1');
          localStorage.setItem(this.USERNAME_KEY, response.username);
          localStorage.setItem(
            this.USER_ROLE_KEY, 
            response.role
          );
        }
        this.isAuthenticatedSubject.next(true);
      })
    );
  }

  logout(): void {
    if (this.isBrowser) {
      localStorage.removeItem(this.TOKEN_KEY);
      localStorage.removeItem(this.ACCOUNT_ID_KEY);
      localStorage.removeItem(this.HOLDER_NAME_KEY);
      localStorage.removeItem(this.USER_ID_KEY);
      localStorage.removeItem(this.USER_ROLE_KEY);
      localStorage.removeItem(this.USERNAME_KEY);
    }
    this.isAuthenticatedSubject.next(false);
  }

  getToken(): string | null {
    if (!this.isBrowser) return null;
    return localStorage.getItem(this.TOKEN_KEY);
  }

  getUsername(): string | null {
    if (!this.isBrowser) return null;
    return localStorage.getItem(this.USERNAME_KEY);
  }

  getCurrentAccountId(): number | null {
    if (!this.isBrowser) return null;
    const accountId = localStorage.getItem(this.ACCOUNT_ID_KEY);
    return accountId ? parseInt(accountId, 10) : null;
  }

  getCurrentUserId(): number | null {
    if (!this.isBrowser) return null;
    const userId = localStorage.getItem(this.USER_ID_KEY);
    return userId ? parseInt(userId, 10) : null;
  }

  getHolderName(): string | null {
    if (!this.isBrowser) return null;
    return localStorage.getItem(this.HOLDER_NAME_KEY);
  }

  getUserRole(): UserRole | null {
    if (!this.isBrowser) return null;
    return localStorage.getItem(this.USER_ROLE_KEY) as UserRole;
  }

  isAdmin(): boolean {
    return this.getUserRole() === UserRole.ADMIN;
  }

  isAuthenticated(): boolean {
    return this.hasToken();
  }

  private hasToken(): boolean {
    if (!this.isBrowser) return false;
    return !!this.getToken();
  }
}