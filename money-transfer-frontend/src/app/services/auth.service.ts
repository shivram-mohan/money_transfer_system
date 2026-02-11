import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
 
export interface LoginRequest {
  username: string;
  password: string;
}
 
export interface LoginResponse {
  token: string;
  accountId: number;
  holderName: string;
}
 
@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly TOKEN_KEY = 'auth_token';
  private readonly ACCOUNT_ID_KEY = 'account_id';
  private readonly HOLDER_NAME_KEY = 'holder_name';
  private readonly USERNAME_KEY = 'username';
  private readonly PASSWORD_KEY = 'password';
  private isAuthenticatedSubject = new BehaviorSubject<boolean>(this.hasToken());
  public isAuthenticated$ = this.isAuthenticatedSubject.asObservable();
 
  constructor(private http: HttpClient) {}
 
  login(credentials: LoginRequest): Observable<any> {
    // For Spring Boot Basic Auth, we just verify credentials work
    // by calling a test endpoint with Basic Auth
    const headers = new HttpHeaders({
      'Authorization': 'Basic ' + btoa(credentials.username + ':' + credentials.password)
    });
 
    // Test authentication by trying to get account 1
    return this.http.get(`${environment.apiUrl}/accounts/1`, { headers }).pipe(
      tap((response: any) => {
        // If successful, store credentials
        localStorage.setItem(this.USERNAME_KEY, credentials.username);
        localStorage.setItem(this.PASSWORD_KEY, credentials.password);
        localStorage.setItem(this.TOKEN_KEY, btoa(credentials.username + ':' + credentials.password));
        localStorage.setItem(this.ACCOUNT_ID_KEY, '1'); // Default to account 1
        localStorage.setItem(this.HOLDER_NAME_KEY, response.holderName || credentials.username);
        this.isAuthenticatedSubject.next(true);
      })
    );
  }
 
  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.ACCOUNT_ID_KEY);
    localStorage.removeItem(this.HOLDER_NAME_KEY);
    localStorage.removeItem(this.USERNAME_KEY);
    localStorage.removeItem(this.PASSWORD_KEY);
    this.isAuthenticatedSubject.next(false);
  }
 
  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }
 
  getUsername(): string | null {
    return localStorage.getItem(this.USERNAME_KEY);
  }
 
  getPassword(): string | null {
    return localStorage.getItem(this.PASSWORD_KEY);
  }
 
  getCurrentAccountId(): number | null {
    const accountId = localStorage.getItem(this.ACCOUNT_ID_KEY);
    return accountId ? parseInt(accountId, 10) : null;
  }
 
  getHolderName(): string | null {
    return localStorage.getItem(this.HOLDER_NAME_KEY);
  }
 
  isAuthenticated(): boolean {
    return this.hasToken();
  }
 
  private hasToken(): boolean {
    return !!this.getToken();
  }
 
  // Get Basic Auth header
  getBasicAuthHeader(): string {
    const username = this.getUsername();
    const password = this.getPassword();
    if (username && password) {
      return 'Basic ' + btoa(username + ':' + password);
    }
    return '';
  }
}