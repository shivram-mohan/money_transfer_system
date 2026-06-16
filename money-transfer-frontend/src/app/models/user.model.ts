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

export interface CreateUserRequest {
  username: string;
  password: string;
  name: string;
  email?: string;
  initialBalance: number;
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
  accountNumber: number;
  username: string;
  email: string;
}

export interface VerifyOtpRequest {
  email: string;
  otp: string;
  purpose: string;
}

export interface SetPasswordRequest {
  accountNumber: number;
  username: string;
  email: string;
  password: string;
}

export interface OtpResponse {
  message: string;
  email: string;
  success: boolean;
}

// ─── New Login Flow DTOs ─────────────────────────────────────────

export interface LoginOtpRequest {
  username: string;
  password: string;
}

export interface LoginVerifyRequest {
  username: string;
  password: string;
  otp: string;
}
