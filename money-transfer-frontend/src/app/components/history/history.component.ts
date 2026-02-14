// src/app/components/history/history.component.ts

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AuthService } from '../../services/auth.service';
import { AccountService } from '../../services/account.service';
import { TransactionLog, TransactionStatus } from '../../models/transaction.model';
import { NavbarComponent } from '../navbar/navbar.component';

interface TransactionDisplay extends TransactionLog {
  type: 'DEBIT' | 'CREDIT';
  displayAmount: number;
}

@Component({
  selector: 'app-history',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    NavbarComponent
  ],
  templateUrl: './history.component.html',
  styleUrls: ['./history.component.scss']
})
export class HistoryComponent implements OnInit {
  transactions: TransactionDisplay[] = [];
  displayedColumns: string[] = ['date', 'type', 'accountDetails', 'amount', 'status']; // ✅ Changed
  isLoading = true;
  currentAccountId: number | null = null;

  constructor(
    private authService: AuthService,
    private accountService: AccountService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.currentAccountId = this.authService.getCurrentAccountId();
    
    if (this.currentAccountId) {
      this.loadTransactions();
    }
  }

  loadTransactions(): void {
    if (this.currentAccountId) {
      this.isLoading = true;
      this.accountService.getTransactions(this.currentAccountId).subscribe({
        next: (transactions) => {
          this.transactions = transactions.map(txn => {
            const isDebit = txn.fromAccountId === this.currentAccountId;
            return {
              ...txn,
              type: isDebit ? 'DEBIT' : 'CREDIT',
              displayAmount: txn.amount
            } as TransactionDisplay;
          });
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Error loading transactions:', error);
          this.isLoading = false;
        }
      });
    }
  }

  getStatusClass(status: TransactionStatus): string {
    return status === TransactionStatus.SUCCESS ? 'status-success' : 'status-failed';
  }

  getTypeClass(type: string): string {
    return type === 'DEBIT' ? 'type-debit' : 'type-credit';
  }

  formatDate(date: Date): string {
    return new Date(date).toLocaleString('en-IN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  refreshHistory(): void {
    this.loadTransactions();
  }

  backToDashboard(): void {
    this.router.navigate(['/dashboard']);
  }
}