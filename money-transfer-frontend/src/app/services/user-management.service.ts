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

  getAllUsers(): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>(this.apiUrl);
  }

  getUserById(userId: number): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.apiUrl}/${userId}`);
  }

  createUser(request: CreateUserRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(this.apiUrl, request);
  }

  activateUser(userId: number): Observable<UserResponse> {
    return this.http.put<UserResponse>(
      `${this.apiUrl}/${userId}/activate`, {}
    );
  }

  deactivateUser(request: DeactivateUserRequest): Observable<UserResponse> {
    return this.http.put<UserResponse>(
      `${this.apiUrl}/deactivate`, request
    );
  }
}
