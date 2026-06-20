// src/app/guards/user.guard.ts

import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Restricts a route to non-admin (regular) users. Admins are accounts for
 * managing users only — they have no bank account and must never reach the
 * user dashboard, transfer or history screens. Admins are bounced back to the
 * admin portal instead.
 */
export const userGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAdmin()) {
    router.navigate(['/admin']);
    return false;
  }

  return true;
};
