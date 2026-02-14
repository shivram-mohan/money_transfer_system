import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { AccountType } from '../../models/account.model';
@Component({
  selector: 'app-account-limits',
  imports: [CommonModule,
    MatCardModule,
    MatIconModule,
    MatProgressBarModule],
  templateUrl: './account-limits.html',
  styleUrl: './account-limits.scss',
})
export class AccountLimits {
@Input() accountType: AccountType = AccountType.SAVINGS;
  @Input() monthlyTransactionCount: number = 0;
  @Input() dailyWithdrawalAmount: number = 0;

  AccountType = AccountType; // Make enum available in template

  get monthlyLimit(): number {
    return this.accountType === AccountType.SAVINGS ? 10 : 0; // 0 = unlimited
  }

  get dailyLimit(): number {
    return this.accountType === AccountType.SAVINGS ? 50000 : 0; // 0 = unlimited
  }

  get monthlyProgress(): number {
    if (this.monthlyLimit === 0) return 0;
    return (this.monthlyTransactionCount / this.monthlyLimit) * 100;
  }

  get dailyProgress(): number {
    if (this.dailyLimit === 0) return 0;
    return (this.dailyWithdrawalAmount / this.dailyLimit) * 100;
  }

  get monthlyProgressColor(): string {
    if (this.monthlyProgress >= 90) return 'warn';
    if (this.monthlyProgress >= 70) return 'accent';
    return 'primary';
  }

  get dailyProgressColor(): string {
    if (this.dailyProgress >= 90) return 'warn';
    if (this.dailyProgress >= 70) return 'accent';
    return 'primary';
  }
}
