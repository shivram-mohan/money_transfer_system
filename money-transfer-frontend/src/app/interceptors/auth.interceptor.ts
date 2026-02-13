// src/app/interceptors/auth.interceptor.ts

import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  
  // Use Basic Auth instead of Bearer token
  const authHeader = authService.getBasicAuthHeader();

  if (authHeader) {
    const clonedRequest = req.clone({
      headers: req.headers.set('Authorization', authHeader)
    });
    return next(clonedRequest);
  }

  return next(req);
};