// src/app/components/dashboard/dashboard.component.ts

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../services/auth.service';
import { AccountService } from '../../services/account.service';
import { NavbarComponent } from '../navbar/navbar.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
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

  constructor(
    private authService: AuthService,
    private accountService: AccountService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.holderName = this.authService.getHolderName();
    this.accountId = this.authService.getCurrentAccountId();

    if (this.accountId != null && this.accountId > 0) {
      this.loadBalance();
    } else {
      this.isLoading = false;
    }
  }

  loadBalance(): void {
    if (this.accountId != null && this.accountId > 0) {
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