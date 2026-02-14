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
  password?: string; // Only used during creation, not stored in responses
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

export interface ApproveUserRequest {
  userId: number;
  approve: boolean;
  remarks?: string;
}

export interface DeactivateUserRequest {
  userId: number;
  reason: string;
}
export interface SignupRequest {
  username: string;
  password: string;
  name: string;
  email?: string;
  initialBalance: number;
}