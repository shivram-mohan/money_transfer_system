import { Injectable, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { UserRole } from '../models/user.model';
import { environment } from '../../environments/environment';

export interface LoginRequest {
  username: string;
  password: string;
  isAdmin?: boolean;
}

export interface AuthMeResponse {
  username: string;
  roles: string[];
  isAdmin: boolean;
  accountId?: number;
  holderName?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly TOKEN_KEY = 'auth_token';
  private readonly ACCOUNT_ID_KEY = 'account_id';
  private readonly HOLDER_NAME_KEY = 'holder_name';
  private readonly USER_ROLE_KEY = 'user_role';
  private readonly USERNAME_KEY = 'username';
  private readonly PASSWORD_KEY = 'password';

  private platformId = inject(PLATFORM_ID);
  private isBrowser: boolean;

  private isAuthenticatedSubject: BehaviorSubject<boolean>;
  public isAuthenticated$: Observable<boolean>;

  constructor(private http: HttpClient) {
    this.isBrowser = isPlatformBrowser(this.platformId);
    this.isAuthenticatedSubject = new BehaviorSubject<boolean>(this.hasToken());
    this.isAuthenticated$ = this.isAuthenticatedSubject.asObservable();
  }

  login(credentials: LoginRequest): Observable<any> {
    const headers = new HttpHeaders({
      'Authorization': 'Basic ' + btoa(
        credentials.username + ':' + credentials.password
      )
    });

    // First validate credentials by calling /auth/me
    return this.http.get<AuthMeResponse>(`${environment.apiUrl}/auth/me`, { headers }).pipe(
      switchMap((authResponse: AuthMeResponse) => {
        const isAdmin = authResponse.isAdmin;
        const role = isAdmin ? UserRole.ADMIN : UserRole.USER;

        // Store credentials for Basic Auth
        if (this.isBrowser) {
          localStorage.setItem(this.USERNAME_KEY, credentials.username);
          localStorage.setItem(this.PASSWORD_KEY, credentials.password);
          localStorage.setItem(
            this.TOKEN_KEY,
            btoa(credentials.username + ':' + credentials.password)
          );
          localStorage.setItem(this.USER_ROLE_KEY, role);
        }

        if (isAdmin) {
          if (this.isBrowser) {
            localStorage.setItem(this.HOLDER_NAME_KEY, 'Admin');
            localStorage.setItem(this.ACCOUNT_ID_KEY, '0');
          }
          this.isAuthenticatedSubject.next(true);

          return new Observable(observer => {
            observer.next({
              holderName: 'Admin',
              role: UserRole.ADMIN,
              accountId: 0
            });
            observer.complete();
          });
        } else {
          // Use accountId and holderName returned from /auth/me
          const accountId = authResponse.accountId;
          const holderName = authResponse.holderName || credentials.username;

          if (this.isBrowser) {
            localStorage.setItem(this.HOLDER_NAME_KEY, holderName);
            localStorage.setItem(this.ACCOUNT_ID_KEY, accountId ? accountId.toString() : '0');
          }
          this.isAuthenticatedSubject.next(true);

          return new Observable(observer => {
            observer.next({
              holderName: holderName,
              role: UserRole.USER,
              accountId: accountId
            });
            observer.complete();
          });
        }
      })
    );
  }

  logout(): void {
    if (this.isBrowser) {
      localStorage.removeItem(this.TOKEN_KEY);
      localStorage.removeItem(this.ACCOUNT_ID_KEY);
      localStorage.removeItem(this.HOLDER_NAME_KEY);
      localStorage.removeItem(this.USER_ROLE_KEY);
      localStorage.removeItem(this.USERNAME_KEY);
      localStorage.removeItem(this.PASSWORD_KEY);
    }
    this.isAuthenticatedSubject.next(false);
  }

  getToken(): string | null {
    if (!this.isBrowser) return null;
    return localStorage.getItem(this.TOKEN_KEY);
  }

  getUsername(): string | null {
    if (!this.isBrowser) return null;
    return localStorage.getItem(this.USERNAME_KEY);
  }

  getPassword(): string | null {
    if (!this.isBrowser) return null;
    return localStorage.getItem(this.PASSWORD_KEY);
  }

  getCurrentAccountId(): number | null {
    if (!this.isBrowser) return null;
    const accountId = localStorage.getItem(this.ACCOUNT_ID_KEY);
    return accountId ? parseInt(accountId, 10) : null;
  }

  getHolderName(): string | null {
    if (!this.isBrowser) return null;
    return localStorage.getItem(this.HOLDER_NAME_KEY);
  }

  getUserRole(): UserRole | null {
    if (!this.isBrowser) return null;
    const role = localStorage.getItem(this.USER_ROLE_KEY);
    return role as UserRole;
  }

  isAdmin(): boolean {
    return this.getUserRole() === UserRole.ADMIN;
  }

  isAuthenticated(): boolean {
    return this.hasToken();
  }

  getBasicAuthHeader(): string {
    const username = this.getUsername();
    const password = this.getPassword();
    if (username && password) {
      return 'Basic ' + btoa(username + ':' + password);
    }
    return '';
  }

  private hasToken(): boolean {
    if (!this.isBrowser) return false;
    return !!this.getToken();
  }
}
