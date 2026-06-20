// src/app/interceptors/auth.interceptor.ts

import { HttpInterceptorFn, HttpRequest, HttpHandlerFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import {
  BehaviorSubject,
  catchError,
  filter,
  switchMap,
  take,
  throwError
} from 'rxjs';
import { AuthService } from '../services/auth.service';

// Shared across requests so concurrent 401s trigger a single refresh.
let isRefreshing = false;
const refreshedToken$ = new BehaviorSubject<string | null>(null);

/** Endpoints that must never trigger a refresh-and-retry cycle. */
function isAuthEndpoint(url: string): boolean {
  return url.includes('/auth/login')
    || url.includes('/auth/admin/login')
    || url.includes('/auth/refresh')
    || url.includes('/auth/signup')
    || url.includes('/auth/forgot-password')
    || url.includes('/auth/reset-password')
    || url.includes('/auth/verify-password');
}

function withToken(req: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return req.clone({
    headers: req.headers.set('Authorization', `Bearer ${token}`)
  });
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const token = authService.getToken();

  const authReq = token ? withToken(req, token) : req;

  return next(authReq).pipe(
    catchError((error) => {
      // Only attempt a refresh for genuine 401s on protected endpoints.
      if (
        error.status !== 401 ||
        isAuthEndpoint(req.url) ||
        !authService.getRefreshToken()
      ) {
        return throwError(() => error);
      }
      return handle401(req, next, authService, router);
    })
  );
};

function handle401(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
  authService: AuthService,
  router: Router
) {
  if (isRefreshing) {
    // Wait for the in-flight refresh, then retry with the new token.
    return refreshedToken$.pipe(
      filter((t): t is string => t !== null),
      take(1),
      switchMap((newToken) => next(withToken(req, newToken)))
    );
  }

  isRefreshing = true;
  refreshedToken$.next(null);

  return authService.refreshToken().pipe(
    switchMap((response) => {
      isRefreshing = false;
      refreshedToken$.next(response.token);
      return next(withToken(req, response.token));
    }),
    catchError((refreshError) => {
      isRefreshing = false;
      // Refresh failed → session is dead. Log out and bounce to login.
      authService.logout();
      router.navigate(['/login']);
      return throwError(() => refreshError);
    })
  );
}
