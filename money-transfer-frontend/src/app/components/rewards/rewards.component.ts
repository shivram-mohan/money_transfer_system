// src/app/components/rewards/rewards.component.ts

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { NavbarComponent } from '../navbar/navbar.component';
import { RewardService } from '../../services/reward.service';
import { RewardProfile } from '../../models/reward.model';
import { RedeemDialogComponent } from './redeem-dialog.component';

@Component({
  selector: 'app-rewards',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatSnackBarModule,
    MatDialogModule,
    NavbarComponent
  ],
  templateUrl: './rewards.component.html',
  styleUrls: ['./rewards.component.scss']
})
export class RewardsComponent implements OnInit {
  profile: RewardProfile | null = null;
  isLoading = true;
  isRedeeming = false;

  constructor(
    private rewardService: RewardService,
    private router: Router,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.loadRewards();
  }

  loadRewards(): void {
    this.isLoading = true;
    this.rewardService.getMyRewards().subscribe({
      next: (profile) => {
        this.profile = profile;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.snackBar.open('Failed to load rewards', 'Close', { duration: 3000 });
      }
    });
  }

  openRedeemDialog(): void {
    if (!this.profile || !this.profile.canRedeem) {
      return;
    }
    const dialogRef = this.dialog.open(RedeemDialogComponent, {
      width: '420px',
      data: {
        balance: this.profile.pointsBalance,
        threshold: this.profile.redeemThreshold
      }
    });

    dialogRef.afterClosed().subscribe((points: number | undefined) => {
      if (points) {
        this.redeem(points);
      }
    });
  }

  private redeem(points: number): void {
    this.isRedeeming = true;
    this.rewardService.redeem(points).subscribe({
      next: (res) => {
        this.isRedeeming = false;
        this.snackBar.open(res.message, 'Close', {
          duration: 5000,
          panelClass: ['success-snackbar']
        });
        this.loadRewards();
      },
      error: (err) => {
        this.isRedeeming = false;
        const msg = err.error?.message || 'Redemption failed. Please try again.';
        this.snackBar.open(msg, 'Close', {
          duration: 5000,
          panelClass: ['error-snackbar']
        });
      }
    });
  }

  backToDashboard(): void {
    this.router.navigate(['/dashboard']);
  }
}
