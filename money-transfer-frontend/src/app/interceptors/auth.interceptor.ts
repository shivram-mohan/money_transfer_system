import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
 
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  // Get Basic Auth credentials
  const authHeader = authService.getBasicAuthHeader();
 
  // Clone the request and add authorization header if credentials exist
  if (authHeader) {
    const clonedRequest = req.clone({
      headers: req.headers.set('Authorization', authHeader)
    });
    return next(clonedRequest);
  }
 
  return next(req);
};