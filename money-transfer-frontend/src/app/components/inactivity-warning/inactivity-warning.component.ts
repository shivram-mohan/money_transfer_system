// src/app/components/inactivity-warning/inactivity-warning.component.ts

import { Component, OnDestroy, OnInit, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  MatDialogModule,
  MatDialogRef,
  MAT_DIALOG_DATA
} from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

export interface InactivityWarningData {
  /** Seconds remaining before automatic logout when the dialog opens. */
  countdownSeconds: number;
}

export type InactivityWarningResult = 'stay' | 'logout';

/**
 * Warns the user that they are about to be logged out for inactivity and shows
 * a live countdown. Returns 'stay' if they choose to remain signed in, or
 * 'logout' if they log out manually. The actual auto-logout deadline is owned
 * by the InactivityService — this dialog is purely presentational.
 */
@Component({
  selector: 'app-inactivity-warning',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule],
  templateUrl: './inactivity-warning.component.html',
  styleUrls: ['./inactivity-warning.component.scss']
})
export class InactivityWarningComponent implements OnInit, OnDestroy {
  remaining: number;
  private intervalId: ReturnType<typeof setInterval> | null = null;

  constructor(
    private dialogRef: MatDialogRef<InactivityWarningComponent, InactivityWarningResult>,
    @Inject(MAT_DIALOG_DATA) public data: InactivityWarningData
  ) {
    this.remaining = data.countdownSeconds;
  }

  ngOnInit(): void {
    this.intervalId = setInterval(() => {
      this.remaining = Math.max(0, this.remaining - 1);
    }, 1000);
  }

  ngOnDestroy(): void {
    if (this.intervalId !== null) {
      clearInterval(this.intervalId);
    }
  }

  get countdownLabel(): string {
    const minutes = Math.floor(this.remaining / 60);
    const seconds = this.remaining % 60;
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  }

  stayLoggedIn(): void {
    this.dialogRef.close('stay');
  }

  logoutNow(): void {
    this.dialogRef.close('logout');
  }
}
