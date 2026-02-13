// src/app/services/user-management.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  UserResponse,
  CreateUserRequest,
  DeactivateUserRequest
} from '../models/user.model';

@Injectable({
  providedIn: 'root'
})
export class UserManagementService {
  private apiUrl = `${environment.apiUrl}/users`;

  constructor(private http: HttpClient) {}

  // Get all users
  getAllUsers(): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>(this.apiUrl);
  }

  // Get pending users
  getPendingUsers(): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>(`${this.apiUrl}/pending`);
  }

  // Get user by ID
  getUserById(userId: number): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.apiUrl}/${userId}`);
  }

  // Create new user
  createUser(
    request: CreateUserRequest,
    createdBy: string
  ): Observable<UserResponse> {
    return this.http.post<UserResponse>(this.apiUrl, request);
  }

  // Activate user
  activateUser(userId: number): Observable<UserResponse> {
    return this.http.put<UserResponse>(
      `${this.apiUrl}/${userId}/activate`, {}
    );
  }

  // Deactivate user
  deactivateUser(
    request: DeactivateUserRequest
  ): Observable<UserResponse> {
    return this.http.put<UserResponse>(
      `${this.apiUrl}/deactivate`, request
    );
  }
}