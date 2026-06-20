// src/app/components/rewards/reward-dialog.component.ts

import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  MatDialogModule,
  MatDialogRef,
  MAT_DIALOG_DATA
} from '@angular/material/dialog';
import { Tier, TIER_META } from '../../models/reward.model';

export interface RewardDialogData {
  variant: 'UPGRADE' | 'CLOSE';
  tier: Tier;          // UPGRADE: the new tier · CLOSE: the next tier
  pointsToNextTier?: number;
}

@Component({
  selector: 'app-reward-dialog',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatDialogModule],
  template: `
    <div class="reward-dialog" [class.upgrade]="data.variant === 'UPGRADE'">
      <div class="confetti" *ngIf="data.variant === 'UPGRADE'">🎉</div>

      <div class="badge"
           [style.background]="meta.gradient"
           [style.box-shadow]="meta.glow">
        <mat-icon>{{ meta.icon }}</mat-icon>
      </div>

      <h2 [style.color]="meta.color">
        {{ data.variant === 'UPGRADE' ? 'Tier Unlocked!' : 'Almost there!' }}
      </h2>

      <p *ngIf="data.variant === 'UPGRADE'">
        You've reached <strong [style.color]="meta.color">{{ meta.label }}</strong> tier.
        Enjoy your boosted points multiplier!
      </p>
      <p *ngIf="data.variant === 'CLOSE'">
        Just <strong [style.color]="meta.color">{{ data.pointsToNextTier }}</strong>
        points away from <strong [style.color]="meta.color">{{ meta.label }}</strong>.
        Keep transferring to unlock it!
      </p>

      <div class="actions">
        <button mat-button (click)="close()">Got it</button>
        <button mat-raised-button color="primary" (click)="viewRewards()">
          <mat-icon>emoji_events</mat-icon>
          View Rewards
        </button>
      </div>
    </div>
  `,
  styles: [`
    .reward-dialog {
      position: relative;
      text-align: center;
      padding: 12px 8px 4px;
    }
    .confetti {
      font-size: 40px;
      margin-bottom: 4px;
      animation: pop 0.6s ease;
    }
    @keyframes pop {
      0% { transform: scale(0); }
      70% { transform: scale(1.3); }
      100% { transform: scale(1); }
    }
    .badge {
      width: 96px;
      height: 96px;
      border-radius: 50%;
      margin: 8px auto 16px;
      display: flex;
      align-items: center;
      justify-content: center;
      animation: float 3s ease-in-out infinite;
    }
    @keyframes float {
      0%, 100% { transform: translateY(0); }
      50% { transform: translateY(-6px); }
    }
    .badge mat-icon {
      font-size: 52px;
      width: 52px;
      height: 52px;
      color: rgba(0, 0, 0, 0.78);
    }
    h2 { margin: 0 0 8px; font-weight: 800; }
    p {
      margin: 0 auto 16px;
      max-width: 320px;
      color: var(--aurora-text-secondary);
      line-height: 1.5;
    }
    .actions {
      display: flex;
      justify-content: center;
      gap: 12px;
      padding-bottom: 8px;
    }
  `]
})
export class RewardDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<RewardDialogComponent>,
    private router: Router,
    @Inject(MAT_DIALOG_DATA) public data: RewardDialogData
  ) {}

  get meta() {
    return TIER_META[this.data.tier];
  }

  close(): void {
    this.dialogRef.close();
  }

  viewRewards(): void {
    this.dialogRef.close();
    this.router.navigate(['/rewards']);
  }
}
