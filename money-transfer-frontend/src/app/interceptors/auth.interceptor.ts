// src/app/interceptors/auth.interceptor.ts

import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getToken();

  // Add Bearer token to all requests
  if (token) {
    const clonedRequest = req.clone({
      headers: req.headers.set(
        'Authorization', 
        `Bearer ${token}`  // ← JWT Bearer token
      )
    });
    return next(clonedRequest);
  }

  return next(req);
};