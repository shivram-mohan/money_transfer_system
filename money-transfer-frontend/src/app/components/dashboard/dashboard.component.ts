// src/app/components/dashboard/dashboard.component.ts

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AuthService } from '../../services/auth.service';
import { AccountService } from '../../services/account.service';
import { RewardService } from '../../services/reward.service';
import { RewardResponse } from '../../models/reward.model';
import { NavbarComponent } from '../navbar/navbar.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    NavbarComponent
  ],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  holderName: string | null = null;
  accountId: number | null = null;
  balance: number = 0;
  isLoading = true;

  // Bank linking
  isBankLinked = false;
  isLinking = false;
  linkForm: FormGroup;

  // Rewards
  rewards: RewardResponse | null = null;

  constructor(
    private authService: AuthService,
    private accountService: AccountService,
    private rewardService: RewardService,
    private router: Router,
    private fb: FormBuilder,
    private snackBar: MatSnackBar
  ) {
    this.linkForm = this.fb.group({
      accountNumber: ['', [Validators.required, Validators.min(1)]]
    });
  }

  ngOnInit(): void {
    this.holderName = this.authService.getHolderName();
    this.accountId = this.authService.getCurrentAccountId();
    this.isBankLinked = this.authService.isBankLinked();

    if (this.isBankLinked && this.accountId) {
      this.loadBalance();
      this.loadRewards();
    } else {
      this.isLoading = false;
    }
  }

  loadRewards(): void {
    if (this.accountId) {
      this.rewardService.getRewards(this.accountId).subscribe({
        next: (rewards) => (this.rewards = rewards),
        error: (error) => console.error('Error loading rewards:', error)
      });
    }
  }

  loadBalance(): void {
    if (this.accountId) {
      this.isLoading = true;
      this.accountService.getBalance(this.accountId).subscribe({
        next: (response: number) => {
          this.balance = response;
          this.isLoading = false;
        },
        error: (error: any) => {
          console.error('Error loading balance:', error);
          this.isLoading = false;
        }
      });
    }
  }

  linkBankAccount(): void {
    if (this.linkForm.invalid) {
      this.linkForm.markAllAsTouched();
      return;
    }

    this.isLinking = true;
    this.accountService.linkBankAccount({
      accountNumber: this.linkForm.value.accountNumber
    }).subscribe({
      next: (response) => {
        this.isLinking = false;
        this.authService.setLinkedAccount(response.accountId, response.holderName);
        this.holderName = response.holderName;
        this.accountId = response.accountId;
        this.balance = response.balance;
        this.isBankLinked = true;
        this.loadRewards();
        this.snackBar.open(
          'Bank account linked! All features are now unlocked.',
          'Close',
          { duration: 5000, panelClass: ['success-snackbar'] }
        );
      },
      error: (error) => {
        this.isLinking = false;
        this.snackBar.open(
          error.error?.message || 'Failed to link bank account',
          'Close',
          { duration: 5000, panelClass: ['error-snackbar'] }
        );
      }
    });
  }

  navigateToTransfer(): void {
    this.router.navigate(['/transfer']);
  }

  navigateToHistory(): void {
    this.router.navigate(['/history']);
  }

  refreshBalance(): void {
    this.loadBalance();
  }
}
