import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { UserResponse, UserStatus, UserRole, CreateUserRequest } from '../models/user.model';
import { AccountResponse } from '../models/account.model';
import { CreateAccountRequest } from './account.service';

@Injectable({
  providedIn: 'root'
})
export class UserManagementService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getAllUsers(): Observable<UserResponse[]> {
    return this.http.get<AccountResponse[]>(`${this.apiUrl}/accounts`).pipe(
      map(accounts => accounts.map(account => this.accountToUserResponse(account)))
    );
  }

  getUserById(userId: number): Observable<UserResponse> {
    return this.http.get<AccountResponse>(`${this.apiUrl}/accounts/${userId}`).pipe(
      map(account => this.accountToUserResponse(account))
    );
  }

  createUser(request: CreateUserRequest, createdBy: string): Observable<UserResponse> {
    const accountRequest: CreateAccountRequest = {
      holderName: request.name,
      username: request.username,
      password: request.password,
      initialBalance: request.initialBalance
    };
    return this.http.post<AccountResponse>(`${this.apiUrl}/accounts`, accountRequest).pipe(
      map(account => this.accountToUserResponse(account))
    );
  }

  deactivateUser(request: { userId: number; reason: string }): Observable<UserResponse> {
    return this.http.put<AccountResponse>(
      `${this.apiUrl}/accounts/${request.userId}/deactivate`, {}
    ).pipe(
      map(account => this.accountToUserResponse(account))
    );
  }

  activateUser(userId: number): Observable<UserResponse> {
    return this.http.put<AccountResponse>(
      `${this.apiUrl}/accounts/${userId}/activate`, {}
    ).pipe(
      map(account => this.accountToUserResponse(account))
    );
  }

  private accountToUserResponse(account: AccountResponse): UserResponse {
    let status: UserStatus;
    switch (account.status) {
      case 'ACTIVE':
        status = UserStatus.ACTIVE;
        break;
      case 'LOCKED':
        status = UserStatus.INACTIVE;
        break;
      case 'CLOSED':
        status = UserStatus.INACTIVE;
        break;
      default:
        status = UserStatus.ACTIVE;
    }

    return {
      id: account.id,
      username: account.holderName.toLowerCase().replace(/\s+/g, '.'),
      name: account.holderName,
      role: UserRole.USER,
      status: status,
      accountId: account.id,
      createdDate: new Date()
    };
  }
}
