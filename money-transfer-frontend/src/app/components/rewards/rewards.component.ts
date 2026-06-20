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
import { NavbarComponent } from '../navbar/navbar.component';
import { RewardService } from '../../services/reward.service';
import {
  RewardProfile,
  MonthlySummary,
  Tier,
  TierMeta,
  TIER_META
} from '../../models/reward.model';

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
    NavbarComponent
  ],
  templateUrl: './rewards.component.html',
  styleUrls: ['./rewards.component.scss']
})
export class RewardsComponent implements OnInit {
  profile: RewardProfile | null = null;
  summary: MonthlySummary | null = null;
  isLoading = true;

  readonly tierOrder: Tier[] = ['BRONZE', 'SILVER', 'GOLD', 'PLATINUM'];
  readonly tierMeta = TIER_META;

  constructor(
    private rewardService: RewardService,
    private router: Router,
    private snackBar: MatSnackBar
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

    this.rewardService.getMonthlySummary().subscribe({
      next: (summary) => (this.summary = summary),
      error: () => {/* summary is non-critical */}
    });
  }

  meta(tier: Tier): TierMeta {
    return this.tierMeta[tier];
  }

  multiplierFor(tier: Tier): string {
    const map: Record<Tier, string> = {
      BRONZE: '1x',
      SILVER: '1.5x',
      GOLD: '2x',
      PLATINUM: '3x'
    };
    return map[tier];
  }

  isCurrent(tier: Tier): boolean {
    return this.profile?.tier === tier;
  }

  isUnlocked(tier: Tier): boolean {
    if (!this.profile) return false;
    return this.tierOrder.indexOf(tier) <= this.tierOrder.indexOf(this.profile.tier);
  }

  backToDashboard(): void {
    this.router.navigate(['/dashboard']);
  }
}
