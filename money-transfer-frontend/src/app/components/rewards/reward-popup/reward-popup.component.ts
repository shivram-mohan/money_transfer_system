import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RewardEarnResult } from '../../../models/reward.model';

/**
 * Celebratory popup shown after a reward-earning transfer. Adapts its headline
 * to the situation: a tier upgrade, an "almost there" nudge, or a plain
 * points-earned confirmation.
 */
@Component({
  selector: 'app-reward-popup',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <div class="reward-popup" [style.--tier-color]="r.rewards.tierColor">
      <div class="glow"></div>

      <div class="badge" [style.background]="r.rewards.tierColor">
        <span class="emoji">{{ r.rewards.tierIcon }}</span>
      </div>

      <h2 class="headline">{{ headline }}</h2>
      <p class="subhead" *ngIf="subhead">{{ subhead }}</p>

      <div class="points-earned">
        <span class="plus">+{{ r.pointsEarned }}</span>
        <span class="label">reward point{{ r.pointsEarned === 1 ? '' : 's' }}</span>
      </div>

      <div class="tier-chip" [style.background]="r.rewards.tierColor">
        {{ r.rewards.tierName }} Tier · {{ r.rewards.multiplier }}x
      </div>

      <ng-container *ngIf="r.rewards.nextTier; else topTier">
        <div class="progress-block">
          <div class="progress-labels">
            <span>{{ r.rewards.points | number }} pts</span>
            <span>{{ r.rewards.nextTier }}</span>
          </div>
          <div class="progress-track">
            <div class="progress-fill" [style.width.%]="r.rewards.progressPercent"></div>
          </div>
          <p class="to-next">
            <mat-icon>flag</mat-icon>
            {{ r.rewards.pointsToNextTier | number }} points to {{ r.rewards.nextTier }}
          </p>
        </div>
      </ng-container>
      <ng-template #topTier>
        <p class="top-tier">🚀 You're at the top tier — maximum multiplier unlocked!</p>
      </ng-template>

      <div class="balance">Current balance: <b>{{ r.rewards.points | number }} pts</b></div>

      <button mat-flat-button class="close-btn" (click)="close()">Awesome!</button>
    </div>
  `,
  styles: [`
    .reward-popup {
      position: relative;
      padding: 28px 24px 22px;
      text-align: center;
      overflow: hidden;
    }
    .glow {
      position: absolute;
      top: -60px; left: 50%;
      transform: translateX(-50%);
      width: 220px; height: 220px;
      background: radial-gradient(circle, var(--tier-color) 0%, transparent 70%);
      opacity: 0.35;
      pointer-events: none;
    }
    .badge {
      width: 76px; height: 76px;
      border-radius: 50%;
      margin: 0 auto 16px;
      display: flex; align-items: center; justify-content: center;
      box-shadow: 0 8px 24px rgba(0,0,0,0.25);
      animation: pop 0.5s ease;
    }
    .emoji { font-size: 40px; line-height: 1; }
    .headline {
      margin: 0 0 4px;
      font-size: 22px;
      font-weight: 700;
      color: var(--aurora-text, #f1f5f9);
    }
    .subhead {
      margin: 0 0 12px;
      font-size: 14px;
      color: var(--aurora-text-secondary, #94a3b8);
    }
    .points-earned {
      display: flex; align-items: baseline; justify-content: center;
      gap: 8px; margin: 12px 0 14px;
    }
    .points-earned .plus {
      font-size: 44px; font-weight: 800;
      background: linear-gradient(90deg, #6366f1, #8b5cf6);
      -webkit-background-clip: text; background-clip: text;
      -webkit-text-fill-color: transparent;
    }
    .points-earned .label { font-size: 14px; color: var(--aurora-text-secondary, #94a3b8); }
    .tier-chip {
      display: inline-block;
      padding: 6px 16px;
      border-radius: 999px;
      font-size: 13px; font-weight: 600; color: #0f172a;
      margin-bottom: 18px;
    }
    .progress-block { margin: 4px 0 16px; }
    .progress-labels {
      display: flex; justify-content: space-between;
      font-size: 12px; color: var(--aurora-text-secondary, #94a3b8);
      margin-bottom: 6px;
    }
    .progress-track {
      background: rgba(148,163,184,0.25);
      border-radius: 999px; height: 12px; overflow: hidden;
    }
    .progress-fill {
      height: 12px; border-radius: 999px;
      background: linear-gradient(90deg, #6366f1, #8b5cf6);
      transition: width 0.8s ease;
    }
    .to-next {
      display: flex; align-items: center; justify-content: center; gap: 4px;
      margin: 10px 0 0; font-size: 13px; color: var(--aurora-text-secondary, #94a3b8);
    }
    .to-next mat-icon { font-size: 16px; width: 16px; height: 16px; }
    .top-tier {
      font-size: 14px; color: #16a34a; font-weight: 600; margin: 4px 0 16px;
    }
    .balance {
      font-size: 13px; color: var(--aurora-text-secondary, #94a3b8);
      margin-bottom: 18px;
    }
    .close-btn {
      width: 100%;
      background: linear-gradient(135deg, #6366f1, #8b5cf6);
      color: #fff; font-weight: 600;
    }
    @keyframes pop {
      0% { transform: scale(0.3); opacity: 0; }
      60% { transform: scale(1.1); }
      100% { transform: scale(1); opacity: 1; }
    }
  `]
})
export class RewardPopupComponent {
  r: RewardEarnResult;
  headline = '';
  subhead = '';

  constructor(
    public dialogRef: MatDialogRef<RewardPopupComponent>,
    @Inject(MAT_DIALOG_DATA) data: RewardEarnResult
  ) {
    this.r = data;
    if (data.tierUpgraded) {
      this.headline = `Welcome to ${data.rewards.tierName}!`;
      this.subhead = `You've been upgraded to the ${data.rewards.tierName} tier.`;
    } else if (data.nearNextTier && data.rewards.nextTier) {
      this.headline = 'Almost there!';
      this.subhead = `Just ${data.rewards.pointsToNextTier} points to reach ${data.rewards.nextTier}.`;
    } else {
      this.headline = 'Points earned!';
      this.subhead = '';
    }
  }

  close(): void {
    this.dialogRef.close();
  }
}
