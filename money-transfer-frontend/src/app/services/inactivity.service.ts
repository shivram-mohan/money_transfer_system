// src/app/services/inactivity.service.ts

import { Injectable, NgZone, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Router } from '@angular/router';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AuthService } from './auth.service';
import {
  InactivityWarningComponent,
  InactivityWarningResult
} from '../components/inactivity-warning/inactivity-warning.component';

/**
 * Logs the user out after a period of inactivity. A warning dialog with a live
 * countdown appears two minutes before the deadline, giving the user a chance
 * to stay signed in. Any user interaction (mouse, keyboard, touch, scroll)
 * resets the idle timer while no warning is showing.
 */
@Injectable({ providedIn: 'root' })
export class InactivityService {
  /** Total idle time before logout. */
  private readonly LOGOUT_AFTER_MS = 10 * 60 * 1000;
  /** Idle time before the warning dialog appears. */
  private readonly WARNING_AFTER_MS = 8 * 60 * 1000;
  /** Lead time the warning gives before logout, in seconds. */
  private readonly WARNING_LEAD_SECONDS =
    (this.LOGOUT_AFTER_MS - this.WARNING_AFTER_MS) / 1000;

  private readonly ACTIVITY_EVENTS = [
    'mousemove', 'mousedown', 'keydown', 'scroll', 'touchstart', 'click'
  ];

  private platformId = inject(PLATFORM_ID);
  private isBrowser = isPlatformBrowser(this.platformId);

  private monitoring = false;
  private warningTimer: ReturnType<typeof setTimeout> | null = null;
  private logoutTimer: ReturnType<typeof setTimeout> | null = null;
  private dialogRef: MatDialogRef<InactivityWarningComponent, InactivityWarningResult> | null = null;

  private readonly activityHandler = () => this.onActivity();

  constructor(
    private zone: NgZone,
    private authService: AuthService,
    private dialog: MatDialog,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  start(): void {
    if (!this.isBrowser || this.monitoring) {
      return;
    }
    this.monitoring = true;

    // Register listeners outside Angular so routine activity doesn't trigger
    // change detection on every mouse move.
    this.zone.runOutsideAngular(() => {
      this.ACTIVITY_EVENTS.forEach((event) =>
        document.addEventListener(event, this.activityHandler, true)
      );
    });

    this.scheduleTimers();
  }

  stop(): void {
    if (!this.isBrowser || !this.monitoring) {
      return;
    }
    this.monitoring = false;

    this.ACTIVITY_EVENTS.forEach((event) =>
      document.removeEventListener(event, this.activityHandler, true)
    );

    this.clearTimers();
    this.closeDialog();
  }

  private onActivity(): void {
    // While the warning dialog is up, only the dialog buttons (or the final
    // timer) decide what happens — background activity is ignored.
    if (this.dialogRef) {
      return;
    }
    this.scheduleTimers();
  }

  private scheduleTimers(): void {
    this.clearTimers();

    this.warningTimer = setTimeout(
      () => this.zone.run(() => this.showWarning()),
      this.WARNING_AFTER_MS
    );
    this.logoutTimer = setTimeout(
      () => this.zone.run(() => this.logoutForInactivity()),
      this.LOGOUT_AFTER_MS
    );
  }

  private showWarning(): void {
    if (this.dialogRef) {
      return;
    }

    this.dialogRef = this.dialog.open(InactivityWarningComponent, {
      width: '380px',
      disableClose: true,
      data: { countdownSeconds: this.WARNING_LEAD_SECONDS }
    });

    this.dialogRef.afterClosed().subscribe((result) => {
      this.dialogRef = null;
      if (result === 'stay') {
        // User is back — restart the idle cycle.
        this.scheduleTimers();
      } else if (result === 'logout') {
        this.logoutForInactivity();
      }
      // result === undefined means logoutForInactivity() closed it; do nothing.
    });
  }

  private logoutForInactivity(): void {
    this.clearTimers();
    this.closeDialog();
    this.monitoring = false;
    this.ACTIVITY_EVENTS.forEach((event) =>
      document.removeEventListener(event, this.activityHandler, true)
    );

    this.authService.logout();
    this.router.navigate(['/login']);
    this.snackBar.open(
      'You were logged out due to inactivity.',
      'Close',
      { duration: 5000 }
    );
  }

  private clearTimers(): void {
    if (this.warningTimer !== null) {
      clearTimeout(this.warningTimer);
      this.warningTimer = null;
    }
    if (this.logoutTimer !== null) {
      clearTimeout(this.logoutTimer);
      this.logoutTimer = null;
    }
  }

  private closeDialog(): void {
    if (this.dialogRef) {
      this.dialogRef.close();
      this.dialogRef = null;
    }
  }
}
