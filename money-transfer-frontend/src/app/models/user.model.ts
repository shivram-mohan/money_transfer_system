// src/app/models/user.model.ts

export enum UserStatus {
  PENDING = 'PENDING',
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  LOCKED = 'LOCKED'
}

export enum UserRole {
  USER = 'USER',
  ADMIN = 'ADMIN'
}

export interface User {
  id: number;
  username: string;
  password?: string;
  name: string;
  role: UserRole;
  status: UserStatus;
  accountId: number;
  email?: string;
  createdDate: Date;
  lastModifiedDate: Date;
  createdBy?: string;
  approvedBy?: string;
  approvedDate?: Date;
}

export interface UserResponse {
  id: number;
  username: string;
  name: string;
  role: UserRole;
  status: UserStatus;
  accountId: number;
  email?: string;
  createdDate: Date;
  approvedBy?: string;
  approvedDate?: Date;
}

export interface DeactivateUserRequest {
  userId: number;
  reason: string;
}

// ─── New Signup Flow DTOs ────────────────────────────────────────

export interface VerifyAccountRequest {
  username: string;
  email: string;
}

export interface VerifyOtpRequest {
  email: string;
  otp: string;
  purpose: string;
}

export interface SetPasswordRequest {
  username: string;
  email: string;
  name: string;
  password: string;
}

export interface OtpResponse {
  message: string;
  email: string;
  success: boolean;
}

// ─── Link Bank Account (post-signup) ─────────────────────────────

export interface LinkBankRequest {
  accountNumber: number;
}

export interface LinkBankResponse {
  accountId: number;
  holderName: string;
  // AES-encrypted balance of the freshly linked account (decrypted on reveal).
  encryptedBalance: string;
  message: string;
}

// ─── New Login Flow DTOs ─────────────────────────────────────────

export interface LoginOtpRequest {
  username: string;
  password: string;
}

export interface LoginVerifyRequest {
  username: string;
  // Password is intentionally omitted: it was already validated in login step 1
  // (which triggered the OTP). Step 2 only needs the username and OTP.
  otp: string;
}

// ─── Forgot / Reset Password DTOs ────────────────────────────────

export interface ForgotPasswordRequest {
  username: string;
}

export interface ResetPasswordRequest {
  username: string;
  otp: string;
  password: string;
}
