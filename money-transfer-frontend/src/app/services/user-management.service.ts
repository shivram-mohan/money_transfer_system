// src/app/services/user-management.service.ts

import { Injectable } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { delay } from 'rxjs/operators';
import { User, UserStatus, UserRole, CreateUserRequest, UserResponse, ApproveUserRequest, DeactivateUserRequest } from '../models/user.model';

@Injectable({
  providedIn: 'root'
})
export class UserManagementService {
  private mockUsers: User[] = [
    {
      id: 1,
      username: 'john.doe',
      name: 'John Doe',
      role: UserRole.USER,
      status: UserStatus.ACTIVE,
      accountId: 1,
      email: 'john.doe@example.com',
      createdDate: new Date('2024-01-15'),
      lastModifiedDate: new Date('2024-01-15'),
      createdBy: 'admin',
      approvedBy: 'admin',
      approvedDate: new Date('2024-01-15')
    },
    {
      id: 2,
      username: 'jane.smith',
      name: 'Jane Smith',
      role: UserRole.USER,
      status: UserStatus.ACTIVE,
      accountId: 2,
      email: 'jane.smith@example.com',
      createdDate: new Date('2024-01-20'),
      lastModifiedDate: new Date('2024-01-20'),
      createdBy: 'admin',
      approvedBy: 'admin',
      approvedDate: new Date('2024-01-20')
    },
    {
      id: 3,
      username: 'demo',
      name: 'Demo User',
      role: UserRole.USER,
      status: UserStatus.ACTIVE,
      accountId: 1,
      email: 'demo@example.com',
      createdDate: new Date('2024-02-01'),
      lastModifiedDate: new Date('2024-02-01'),
      createdBy: 'admin',
      approvedBy: 'admin',
      approvedDate: new Date('2024-02-01')
    },
    {
      id: 4,
      username: 'bob.wilson',
      name: 'Bob Wilson',
      role: UserRole.USER,
      status: UserStatus.PENDING,
      accountId: 3,
      email: 'bob.wilson@example.com',
      createdDate: new Date('2024-02-10'),
      lastModifiedDate: new Date('2024-02-10'),
      createdBy: 'admin'
    },
    {
      id: 5,
      username: 'alice.brown',
      name: 'Alice Brown',
      role: UserRole.USER,
      status: UserStatus.INACTIVE,
      accountId: 4,
      email: 'alice.brown@example.com',
      createdDate: new Date('2024-01-10'),
      lastModifiedDate: new Date('2024-02-05'),
      createdBy: 'admin',
      approvedBy: 'admin',
      approvedDate: new Date('2024-01-10')
    }
  ];

  private nextUserId = 6;
  private nextAccountId = 5;

  constructor() {}

  // Get all users
  getAllUsers(): Observable<UserResponse[]> {
    const users = this.mockUsers.map(user => this.toUserResponse(user));
    return of(users).pipe(delay(500));
  }

  // Get pending users
  getPendingUsers(): Observable<UserResponse[]> {
    const pendingUsers = this.mockUsers
      .filter(user => user.status === UserStatus.PENDING)
      .map(user => this.toUserResponse(user));
    return of(pendingUsers).pipe(delay(500));
  }

  // Get user by ID
  getUserById(userId: number): Observable<UserResponse> {
    const user = this.mockUsers.find(u => u.id === userId);
    if (user) {
      return of(this.toUserResponse(user)).pipe(delay(300));
    }
    return throwError(() => ({ message: 'User not found' }));
  }

  // Create new user
  createUser(request: CreateUserRequest, createdBy: string): Observable<UserResponse> {
    return new Observable(observer => {
      setTimeout(() => {
        // Check if username already exists
        const existingUser = this.mockUsers.find(u => u.username === request.username);
        if (existingUser) {
          observer.error({ message: 'Username already exists' });
          return;
        }

        // Create new user
        const newUser: User = {
          id: this.nextUserId++,
          username: request.username,
          name: request.name,
          role: UserRole.USER,
          status: UserStatus.ACTIVE, 
          accountId: this.nextAccountId++,
          email: request.email,
          createdDate: new Date(),
          lastModifiedDate: new Date(),
          createdBy: createdBy
        };

        this.mockUsers.push(newUser);

        observer.next(this.toUserResponse(newUser));
        observer.complete();
      }, 800);
    });
  }

  // Approve user
  approveUser(request: ApproveUserRequest, approvedBy: string): Observable<UserResponse> {
    return new Observable(observer => {
      setTimeout(() => {
        const user = this.mockUsers.find(u => u.id === request.userId);
        
        if (!user) {
          observer.error({ message: 'User not found' });
          return;
        }

        if (user.status !== UserStatus.PENDING) {
          observer.error({ message: 'User is not in pending status' });
          return;
        }

        if (request.approve) {
          user.status = UserStatus.ACTIVE;
          user.approvedBy = approvedBy;
          user.approvedDate = new Date();
          user.lastModifiedDate = new Date();
        } else {
          // Reject - remove from list
          const index = this.mockUsers.findIndex(u => u.id === request.userId);
          this.mockUsers.splice(index, 1);
          observer.next({ message: 'User rejected and removed' } as any);
          observer.complete();
          return;
        }

        observer.next(this.toUserResponse(user));
        observer.complete();
      }, 800);
    });
  }

  // Deactivate user
  deactivateUser(request: DeactivateUserRequest): Observable<UserResponse> {
    return new Observable(observer => {
      setTimeout(() => {
        const user = this.mockUsers.find(u => u.id === request.userId);
        
        if (!user) {
          observer.error({ message: 'User not found' });
          return;
        }

        if (user.status === UserStatus.INACTIVE) {
          observer.error({ message: 'User is already inactive' });
          return;
        }

        user.status = UserStatus.INACTIVE;
        user.lastModifiedDate = new Date();

        observer.next(this.toUserResponse(user));
        observer.complete();
      }, 800);
    });
  }

  // Activate user
  activateUser(userId: number): Observable<UserResponse> {
    return new Observable(observer => {
      setTimeout(() => {
        const user = this.mockUsers.find(u => u.id === userId);
        
        if (!user) {
          observer.error({ message: 'User not found' });
          return;
        }

        if (user.status === UserStatus.INACTIVE) {
          observer.error({ message: 'User is inactive' });
          return;
        }

        user.status = UserStatus.ACTIVE;
        user.lastModifiedDate = new Date();

        observer.next(this.toUserResponse(user));
        observer.complete();
      }, 800);
    });
  }

  // Helper method to convert User to UserResponse
  private toUserResponse(user: User): UserResponse {
    return {
      id: user.id,
      username: user.username,
      name: user.name,
      role: user.role,
      status: user.status,
      accountId: user.accountId,
      email: user.email,
      createdDate: user.createdDate,
      approvedBy: user.approvedBy,
      approvedDate: user.approvedDate
    };
  }
}