// src/app/services/auth.service.ts

import { Injectable, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
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

export interface LoginResponse {
  token: string;
  refreshToken: string;
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
  private readonly REFRESH_TOKEN_KEY = 'refresh_token';
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
    // Password is sent as-is over HTTPS; the backend bcrypt-encodes it for storage
    return this.http.post<UserResponse>(
      `${environment.apiUrl}/auth/signup/set-password`,
      request
    );
  }

  // ─── USER LOGIN FLOW (2 steps) ─────────────────────────────────

  loginStep1(request: LoginOtpRequest): Observable<OtpResponse> {
    return this.http.post<OtpResponse>(
      `${environment.apiUrl}/auth/login`,
      request
    );
  }

  loginVerifyOtp(request: LoginVerifyRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(
      `${environment.apiUrl}/auth/login/verify-otp`,
      request
    ).pipe(
      tap((response: LoginResponse) => this.persistSession(response))
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
    // Password is sent as-is over HTTPS; the backend bcrypt-encodes it for storage
    return this.http.post<OtpResponse>(
      `${environment.apiUrl}/auth/reset-password`,
      request
    );
  }

  // ─── ADMIN LOGIN (direct, no OTP) ──────────────────────────────

  adminLogin(credentials: AdminLoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(
      `${environment.apiUrl}/auth/admin/login`,
      credentials
    ).pipe(
      tap((response: LoginResponse) => this.persistSession(response))
    );
  }

  // ─── TOKEN REFRESH ─────────────────────────────────────────────

  /**
   * Exchanges the stored refresh token for a fresh access token. The new
   * tokens are persisted so subsequent requests use them transparently.
   */
  refreshToken(): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(
      `${environment.apiUrl}/auth/refresh`,
      { refreshToken: this.getRefreshToken() }
    ).pipe(
      tap((response: LoginResponse) => this.persistSession(response))
    );
  }

  // ─── PASSWORD RE-VERIFICATION (sensitive actions) ──────────────

  /**
   * Re-checks the current user's login password (e.g. before revealing the
   * balance). Resolves to true when the password is correct.
   */
  verifyPassword(password: string): Observable<OtpResponse> {
    const username = this.getUsername() ?? '';
    return this.http.post<OtpResponse>(
      `${environment.apiUrl}/auth/verify-password`,
      { username, password }
    );
  }

  // ─── SESSION MANAGEMENT ────────────────────────────────────────

  private persistSession(response: LoginResponse): void {
    if (this.isBrowser) {
      sessionStorage.setItem(this.TOKEN_KEY, response.token);
      sessionStorage.setItem(this.REFRESH_TOKEN_KEY, response.refreshToken);
      sessionStorage.setItem(
        this.ACCOUNT_ID_KEY,
        response.accountId?.toString() || '0'
      );
      sessionStorage.setItem(this.HOLDER_NAME_KEY, response.holderName);
      sessionStorage.setItem(
        this.USER_ID_KEY,
        response.userId?.toString() || '1'
      );
      sessionStorage.setItem(this.USERNAME_KEY, response.username);
      sessionStorage.setItem(this.USER_ROLE_KEY, response.role);
    }
    this.isAuthenticatedSubject.next(true);
  }

  logout(): void {
    if (this.isBrowser) {
      sessionStorage.removeItem(this.TOKEN_KEY);
      sessionStorage.removeItem(this.REFRESH_TOKEN_KEY);
      sessionStorage.removeItem(this.ACCOUNT_ID_KEY);
      sessionStorage.removeItem(this.HOLDER_NAME_KEY);
      sessionStorage.removeItem(this.USER_ID_KEY);
      sessionStorage.removeItem(this.USER_ROLE_KEY);
      sessionStorage.removeItem(this.USERNAME_KEY);
    }
    this.isAuthenticatedSubject.next(false);
  }

  getToken(): string | null {
    if (!this.isBrowser) return null;
    return sessionStorage.getItem(this.TOKEN_KEY);
  }

  getRefreshToken(): string | null {
    if (!this.isBrowser) return null;
    return sessionStorage.getItem(this.REFRESH_TOKEN_KEY);
  }

  getUsername(): string | null {
    if (!this.isBrowser) return null;
    return sessionStorage.getItem(this.USERNAME_KEY);
  }

  getCurrentAccountId(): number | null {
    if (!this.isBrowser) return null;
    const accountId = sessionStorage.getItem(this.ACCOUNT_ID_KEY);
    const parsed = accountId ? parseInt(accountId, 10) : NaN;
    return Number.isNaN(parsed) || parsed <= 0 ? null : parsed;
  }

  isBankLinked(): boolean {
    return this.getCurrentAccountId() !== null;
  }

  // Called after the user links a bank account post-signup
  setLinkedAccount(accountId: number, holderName: string): void {
    if (this.isBrowser) {
      sessionStorage.setItem(this.ACCOUNT_ID_KEY, accountId.toString());
      sessionStorage.setItem(this.HOLDER_NAME_KEY, holderName);
    }
  }

  getCurrentUserId(): number | null {
    if (!this.isBrowser) return null;
    const userId = sessionStorage.getItem(this.USER_ID_KEY);
    return userId ? parseInt(userId, 10) : null;
  }

  getHolderName(): string | null {
    if (!this.isBrowser) return null;
    return sessionStorage.getItem(this.HOLDER_NAME_KEY);
  }

  getUserRole(): UserRole | null {
    if (!this.isBrowser) return null;
    return sessionStorage.getItem(this.USER_ROLE_KEY) as UserRole;
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
