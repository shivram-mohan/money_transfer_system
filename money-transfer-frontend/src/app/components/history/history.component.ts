// src/app/components/history/history.component.ts

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AuthService } from '../../services/auth.service';
import { AccountService } from '../../services/account.service';
import { TransactionLog, TransactionStatus } from '../../models/transaction.model';
import { NavbarComponent } from '../navbar/navbar.component';

interface TransactionDisplay extends TransactionLog {
  type: 'DEBIT' | 'CREDIT';
  displayAmount: number;
  isCashback: boolean;
}

type FilterType = 'ALL' | 'LAST_WEEK' | 'LAST_MONTH' | 'LAST_YEAR' | 'CUSTOM';

@Component({
  selector: 'app-history',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatSelectModule,
    MatFormFieldModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatInputModule,
    MatSnackBarModule,
    NavbarComponent
  ],
  templateUrl: './history.component.html',
  styleUrls: ['./history.component.scss']
})
export class HistoryComponent implements OnInit {
  transactions: TransactionDisplay[] = [];
  displayedColumns: string[] = ['date', 'type', 'accountDetails', 'amount', 'status'];
  isLoading = true;
  currentAccountId: number | null = null;

  // Filter controls
  filterControl = new FormControl<FilterType>('ALL');
  startDateControl = new FormControl<Date | null>(null);
  endDateControl = new FormControl<Date | null>(null);
  showCustomDatePicker = false;

  constructor(
    private authService: AuthService,
    private accountService: AccountService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    // Block access until the user has linked a bank account
    if (!this.authService.isBankLinked()) {
      this.snackBar.open(
        'Link a bank account to view transaction history',
        'Close',
        { duration: 4000 }
      );
      this.router.navigate(['/dashboard']);
      return;
    }

    this.currentAccountId = this.authService.getCurrentAccountId();

    if (this.currentAccountId) {
      this.loadTransactions();
      this.setupFilterListener();
    }
  }

  setupFilterListener(): void {
    this.filterControl.valueChanges.subscribe(filterType => {
      if (filterType === 'CUSTOM') {
        this.showCustomDatePicker = true;
      } else {
        this.showCustomDatePicker = false;
        this.applyFilter(filterType!);
      }
    });
  }

  loadTransactions(): void {
    if (this.currentAccountId) {
      this.isLoading = true;
      this.accountService.getTransactions(this.currentAccountId).subscribe({
        next: (transactions) => {
          this.processTransactions(transactions);
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Error loading transactions:', error);
          this.isLoading = false;
          this.snackBar.open('Failed to load transactions', 'Close', {
            duration: 3000
          });
        }
      });
    }
  }

  applyFilter(filterType: FilterType): void {
    if (!this.currentAccountId) return;

    this.isLoading = true;

    switch (filterType) {
      case 'ALL':
        this.loadTransactions();
        break;

      case 'LAST_WEEK':
        this.accountService.getLastWeekTransactions(this.currentAccountId)
          .subscribe({
            next: (transactions) => {
              this.processTransactions(transactions);
              this.isLoading = false;
            },
            error: (error) => this.handleFilterError(error)
          });
        break;

      case 'LAST_MONTH':
        this.accountService.getLastMonthTransactions(this.currentAccountId)
          .subscribe({
            next: (transactions) => {
              this.processTransactions(transactions);
              this.isLoading = false;
            },
            error: (error) => this.handleFilterError(error)
          });
        break;

      case 'LAST_YEAR':
        this.accountService.getLastYearTransactions(this.currentAccountId)
          .subscribe({
            next: (transactions) => {
              this.processTransactions(transactions);
              this.isLoading = false;
            },
            error: (error) => this.handleFilterError(error)
          });
        break;
    }
  }

  applyCustomDateFilter(): void {
    if (!this.currentAccountId || !this.startDateControl.value || !this.endDateControl.value) {
      this.snackBar.open('Please select both start and end dates', 'Close', {
        duration: 3000
      });
      return;
    }

    this.isLoading = true;

    const startDate = this.formatDate(this.startDateControl.value);
    const endDate = this.formatDate(this.endDateControl.value);

    this.accountService.getFilteredTransactions(
      this.currentAccountId,
      startDate,
      endDate
    ).subscribe({
      next: (transactions) => {
        this.processTransactions(transactions);
        this.isLoading = false;
        this.snackBar.open('Filter applied successfully', 'Close', {
          duration: 2000
        });
      },
      error: (error) => this.handleFilterError(error)
    });
  }

  downloadPdf(): void {
    if (!this.currentAccountId) return;

    let startDate: string | undefined;
    let endDate: string | undefined;

    // Determine date range based on current filter
    if (this.filterControl.value === 'CUSTOM' && 
        this.startDateControl.value && 
        this.endDateControl.value) {
      startDate = this.formatDate(this.startDateControl.value);
      endDate = this.formatDate(this.endDateControl.value);
    }

    this.snackBar.open('Generating PDF...', '', { duration: 2000 });

    this.accountService.downloadPdfStatement(
      this.currentAccountId,
      startDate,
      endDate
    ).subscribe({
      next: (blob) => {
        // Create download link
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        
        const fileName = `statement_${this.currentAccountId}_${
          startDate || 'last_month'
        }_to_${endDate || 'today'}.pdf`;
        
        link.download = fileName;
        link.click();
        
        window.URL.revokeObjectURL(url);
        
        this.snackBar.open('PDF downloaded successfully!', 'Close', {
          duration: 3000
        });
      },
      error: (error) => {
        console.error('Error downloading PDF:', error);
        this.snackBar.open('Failed to download PDF', 'Close', {
          duration: 3000
        });
      }
    });
  }

  private processTransactions(transactions: TransactionLog[]): void {
    this.transactions = transactions.map(txn => {
      const isDebit = txn.fromAccountId === this.currentAccountId;
      // Cashback payouts arrive as a CREDIT from the masked "CASHBACK" account.
      const isCashback = !isDebit && txn.fromAccountHolderName === 'CASHBACK';
      return {
        ...txn,
        type: isDebit ? 'DEBIT' : 'CREDIT',
        displayAmount: txn.amount,
        isCashback
      } as TransactionDisplay;
    });
  }

  private handleFilterError(error: any): void {
    console.error('Error applying filter:', error);
    this.isLoading = false;
    this.snackBar.open('Failed to apply filter', 'Close', {
      duration: 3000
    });
  }

  private formatDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  getStatusClass(status: TransactionStatus): string {
    return status === TransactionStatus.SUCCESS ? 'status-success' : 'status-failed';
  }

  getTypeClass(type: string): string {
    return type === 'DEBIT' ? 'type-debit' : 'type-credit';
  }

  formatDate2(date: Date): string {
    return new Date(date).toLocaleString('en-IN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  refreshHistory(): void {
    this.filterControl.setValue('ALL');
    this.loadTransactions();
  }

  backToDashboard(): void {
    this.router.navigate(['/dashboard']);
  }
}