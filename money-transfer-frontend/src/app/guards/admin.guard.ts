import { Injectable, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { BehaviorSubject, Observable } from 'rxjs';
import { UserRole } from '../models/user.model';

export interface LoginRequest {
  username: string;
  password: string;
  isAdmin?: boolean;
}

export interface LoginResponse {
  token: string;
  accountId: number;
  holderName: string;
  userId: number;
  role: UserRole;
}

@Injectable({
  providedIn: 'root'
})
export class adminGuard {
  private readonly TOKEN_KEY = 'auth_token';
  private readonly ACCOUNT_ID_KEY = 'account_id';
  private readonly HOLDER_NAME_KEY = 'holder_name';
  private readonly USER_ID_KEY = 'user_id';
  private readonly USER_ROLE_KEY = 'user_role';
  
  private platformId = inject(PLATFORM_ID);
  private isBrowser: boolean;
  
  private isAuthenticatedSubject: BehaviorSubject<boolean>;
  public isAuthenticated$: Observable<boolean>;

  // Hardcoded user credentials for testing
  private mockUsers = [
    { id: 1, username: 'john.doe', password: 'password123', accountId: 1, holderName: 'John Doe', token: 'mock-token-john', role: UserRole.USER },
    { id: 2, username: 'jane.smith', password: 'password123', accountId: 2, holderName: 'Jane Smith', token: 'mock-token-jane', role: UserRole.USER },
    { id: 3, username: 'demo', password: 'demo', accountId: 1, holderName: 'Demo User', token: 'mock-token-demo', role: UserRole.USER }
  ];

  // Single admin user
  private adminUser = {
    id: 100,
    username: 'admin',
    password: 'admin123',
    accountId: 999,
    holderName: 'Admin',
    token: 'mock-token-admin',
    role: UserRole.ADMIN
  };

  constructor() {
    this.isBrowser = isPlatformBrowser(this.platformId);
    this.isAuthenticatedSubject = new BehaviorSubject<boolean>(this.hasToken());
    this.isAuthenticated$ = this.isAuthenticatedSubject.asObservable();
  }

  login(credentials: LoginRequest): Observable<LoginResponse> {
    console.log('Login attempt:', credentials); // Debug log
    
    return new Observable(observer => {
      setTimeout(() => {
        let user = null;
        
        // Check if logging in as admin
        if (credentials.isAdmin) {
          console.log('Checking admin credentials'); // Debug log
          if (this.adminUser.username === credentials.username && 
              this.adminUser.password === credentials.password) {
            user = this.adminUser;
            console.log('Admin login successful'); // Debug log
          }
        } else {
          console.log('Checking user credentials'); // Debug log
          user = this.mockUsers.find(
            u => u.username === credentials.username && u.password === credentials.password
          );
          if (user) {
            console.log('User login successful'); // Debug log
          }
        }

        if (user) {
          const response: LoginResponse = {
            token: user.token,
            accountId: user.accountId,
            holderName: user.holderName,
            userId: user.id,
            role: user.role
          };
          
          console.log('Login response:', response); // Debug log
          
          // Store in localStorage (only in browser)
          if (this.isBrowser) {
            localStorage.setItem(this.TOKEN_KEY, response.token);
            localStorage.setItem(this.ACCOUNT_ID_KEY, response.accountId.toString());
            localStorage.setItem(this.HOLDER_NAME_KEY, response.holderName);
            localStorage.setItem(this.USER_ID_KEY, response.userId.toString());
            localStorage.setItem(this.USER_ROLE_KEY, response.role);
            console.log('Stored role:', response.role); // Debug log
          }
          
          this.isAuthenticatedSubject.next(true);
          observer.next(response);
          observer.complete();
        } else {
          console.log('Login failed - invalid credentials'); // Debug log
          observer.error({ message: 'Invalid username or password' });
        }
      }, 500); // Reduced delay for faster testing
    });
  }

  logout(): void {
    if (this.isBrowser) {
      localStorage.removeItem(this.TOKEN_KEY);
      localStorage.removeItem(this.ACCOUNT_ID_KEY);
      localStorage.removeItem(this.HOLDER_NAME_KEY);
      localStorage.removeItem(this.USER_ID_KEY);
      localStorage.removeItem(this.USER_ROLE_KEY);
    }
    this.isAuthenticatedSubject.next(false);
  }

  getToken(): string | null {
    if (!this.isBrowser) {
      return null;
    }
    return localStorage.getItem(this.TOKEN_KEY);
  }

  getCurrentAccountId(): number | null {
    if (!this.isBrowser) {
      return null;
    }
    const accountId = localStorage.getItem(this.ACCOUNT_ID_KEY);
    return accountId ? parseInt(accountId, 10) : null;
  }

  getCurrentUserId(): number | null {
    if (!this.isBrowser) {
      return null;
    }
    const userId = localStorage.getItem(this.USER_ID_KEY);
    return userId ? parseInt(userId, 10) : null;
  }

  getHolderName(): string | null {
    if (!this.isBrowser) {
      return null;
    }
    return localStorage.getItem(this.HOLDER_NAME_KEY);
  }

  getUserRole(): UserRole | null {
    if (!this.isBrowser) {
      return null;
    }
    const role = localStorage.getItem(this.USER_ROLE_KEY);
    console.log('Getting stored role:', role); // Debug log
    return role as UserRole;
  }

  isAdmin(): boolean {
    const role = this.getUserRole();
    const isAdminUser = role === UserRole.ADMIN;
    console.log('isAdmin check - role:', role, 'isAdmin:', isAdminUser); // Debug log
    return isAdminUser;
  }

  isAuthenticated(): boolean {
    return this.hasToken();
  }

  private hasToken(): boolean {
    if (!this.isBrowser) {
      return false;
    }
    return !!this.getToken();
  }
}