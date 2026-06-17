// src/app/services/user-management.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, from } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import {
  UserResponse,
  CreateUserRequest,
  DeactivateUserRequest
} from '../models/user.model';
import { hashPassword } from '../utils/crypto.util';

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
    // Hash client-side so the password is consistent with the login flow
    return from(hashPassword(request.password)).pipe(
      switchMap((hashed) =>
        this.http.post<UserResponse>(this.apiUrl, { ...request, password: hashed })
      )
    );
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
