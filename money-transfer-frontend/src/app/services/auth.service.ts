// src/app/services/auth.service.ts

import { Injectable, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, from } from 'rxjs';
import { switchMap, tap } from 'rxjs/operators';
import {
  UserRole,
  VerifyAccountRequest,
  VerifyOtpRequest,
  SetPasswordRequest,
  OtpResponse,
  LoginOtpRequest,
  LoginVerifyRequest,
  UserResponse,
  ForgotPasswordRequest,
  ResetPasswordRequest
} from '../models/user.model';
import { environment } from '../../environments/environment';
import { hashPassword } from '../utils/crypto.util';

export interface LoginResponse {
  token: string;
  accountId: number;
  holderName: string;
  userId: number;
  role: UserRole;
  username: string;
  expiresIn: number;
}

export interface AdminLoginRequest {
  username: string;
  password: string;
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

  // ─── SIGNUP FLOW (3 steps) ──────────────────────────────────────

  verifyAccount(request: VerifyAccountRequest): Observable<OtpResponse> {
    return this.http.post<OtpResponse>(
      `${environment.apiUrl}/auth/signup/verify-account`,
      request
    );
  }

  verifySignupOtp(request: VerifyOtpRequest): Observable<OtpResponse> {
    return this.http.post<OtpResponse>(
      `${environment.apiUrl}/auth/signup/verify-otp`,
      request
    );
  }

  setPassword(request: SetPasswordRequest): Observable<UserResponse> {
    // Hash the password client-side so plaintext never leaves the browser
    return from(hashPassword(request.password)).pipe(
      switchMap((hashed) =>
        this.http.post<UserResponse>(
          `${environment.apiUrl}/auth/signup/set-password`,
          { ...request, password: hashed }
        )
      )
    );
  }

  // ─── USER LOGIN FLOW (2 steps) ─────────────────────────────────

  loginStep1(request: LoginOtpRequest): Observable<OtpResponse> {
    return from(hashPassword(request.password)).pipe(
      switchMap((hashed) =>
        this.http.post<OtpResponse>(
          `${environment.apiUrl}/auth/login`,
          { ...request, password: hashed }
        )
      )
    );
  }

  loginVerifyOtp(request: LoginVerifyRequest): Observable<LoginResponse> {
    return from(hashPassword(request.password)).pipe(
      switchMap((hashed) =>
        this.http.post<LoginResponse>(
          `${environment.apiUrl}/auth/login/verify-otp`,
          { ...request, password: hashed }
        )
      )
    ).pipe(
      tap((response: LoginResponse) => {
        if (this.isBrowser) {
          localStorage.setItem(this.TOKEN_KEY, response.token);
          localStorage.setItem(
            this.ACCOUNT_ID_KEY,
            response.accountId?.toString() || '0'
          );
          localStorage.setItem(this.HOLDER_NAME_KEY, response.holderName);
          localStorage.setItem(this.USER_ID_KEY, '1');
          localStorage.setItem(this.USERNAME_KEY, response.username);
          localStorage.setItem(this.USER_ROLE_KEY, response.role);
        }
        this.isAuthenticatedSubject.next(true);
      })
    );
  }

  // ─── FORGOT / RESET PASSWORD ───────────────────────────────────

  forgotPassword(request: ForgotPasswordRequest): Observable<OtpResponse> {
    return this.http.post<OtpResponse>(
      `${environment.apiUrl}/auth/forgot-password`,
      request
    );
  }

  resetPassword(request: ResetPasswordRequest): Observable<OtpResponse> {
    // Hash the new password client-side so plaintext never leaves the browser
    return from(hashPassword(request.password)).pipe(
      switchMap((hashed) =>
        this.http.post<OtpResponse>(
          `${environment.apiUrl}/auth/reset-password`,
          { ...request, password: hashed }
        )
      )
    );
  }

  // ─── ADMIN LOGIN (direct, no OTP) ──────────────────────────────

  adminLogin(credentials: AdminLoginRequest): Observable<LoginResponse> {
    return from(hashPassword(credentials.password)).pipe(
      switchMap((hashed) =>
        this.http.post<LoginResponse>(
          `${environment.apiUrl}/auth/admin/login`,
          { ...credentials, password: hashed }
        )
      )
    ).pipe(
      tap((response: LoginResponse) => {
        if (this.isBrowser) {
          localStorage.setItem(this.TOKEN_KEY, response.token);
          localStorage.setItem(
            this.ACCOUNT_ID_KEY,
            response.accountId?.toString() || '0'
          );
          localStorage.setItem(this.HOLDER_NAME_KEY, response.holderName);
          localStorage.setItem(this.USER_ID_KEY, '1');
          localStorage.setItem(this.USERNAME_KEY, response.username);
          localStorage.setItem(this.USER_ROLE_KEY, response.role);
        }
        this.isAuthenticatedSubject.next(true);
      })
    );
  }

  // ─── SESSION MANAGEMENT ────────────────────────────────────────

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
    const parsed = accountId ? parseInt(accountId, 10) : NaN;
    return Number.isNaN(parsed) || parsed <= 0 ? null : parsed;
  }

  isBankLinked(): boolean {
    return this.getCurrentAccountId() !== null;
  }

  // Called after the user links a bank account post-signup
  setLinkedAccount(accountId: number, holderName: string): void {
    if (this.isBrowser) {
      localStorage.setItem(this.ACCOUNT_ID_KEY, accountId.toString());
      localStorage.setItem(this.HOLDER_NAME_KEY, holderName);
    }
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
