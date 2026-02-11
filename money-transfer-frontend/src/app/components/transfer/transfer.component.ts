// src/app/components/transfer/transfer.component.ts

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { AuthService } from '../../services/auth.service';
import { TransferService } from '../../services/transfer.service';
import { AccountService } from '../../services/account.service';
import { NavbarComponent } from '../navbar/navbar.component';
import { TransferRequest } from '../../models/transaction.model';

@Component({
  selector: 'app-transfer',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDialogModule,
    NavbarComponent
  ],
  templateUrl: './transfer.component.html',
  styleUrls: ['./transfer.component.scss']
})
export class TransferComponent implements OnInit {
  transferForm: FormGroup;
  isLoading = false;
  currentAccountId: number | null = null;
  currentBalance: number = 0;
  holderName: string | null = null;
  transferSuccess = false;
  transferResult: any = null;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private transferService: TransferService,
    private accountService: AccountService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {
    this.transferForm = this.fb.group({
      toAccountId: ['', [Validators.required, Validators.min(1)]],
      amount: ['', [Validators.required, Validators.min(0.01)]]
    });
  }

  ngOnInit(): void {
    this.currentAccountId = this.authService.getCurrentAccountId();
    this.holderName = this.authService.getHolderName();
    
    if (this.currentAccountId) {
      this.loadBalance();
    }
  }

  loadBalance(): void {
    if (this.currentAccountId) {
      this.accountService.getBalance(this.currentAccountId).subscribe({
        next: (response: number) => {
          this.currentBalance = response;
        },
        error: (error: any) => {
          console.error('Error loading balance:', error);
        }
      });
    }
  }

  onSubmit(): void {
    if (this.transferForm.valid && this.currentAccountId) {
      // Additional validations
      const toAccountId = this.transferForm.get('toAccountId')?.value;
      const amount = this.transferForm.get('amount')?.value;

      // Check if transferring to self
      if (toAccountId === this.currentAccountId) {
        this.snackBar.open('Cannot transfer to your own account', 'Close', {
          duration: 5000,
          panelClass: ['error-snackbar']
        });
        return;
      }

      // Check sufficient balance
      if (amount > this.currentBalance) {
        this.snackBar.open('Insufficient balance', 'Close', {
          duration: 5000,
          panelClass: ['error-snackbar']
        });
        return;
      }

      this.isLoading = true;
      this.transferSuccess = false;

      const transferRequest: TransferRequest = {
        fromAccountId: this.currentAccountId,
        toAccountId: toAccountId,
        amount: amount,
        idempotencyKey: this.transferService.generateIdempotencyKey()
      };

      this.transferService.transfer(transferRequest).subscribe({
        next: (response: any) => {
          this.isLoading = false;
          this.transferSuccess = true;
          this.transferResult = response;
          this.snackBar.open('Transfer completed successfully!', 'Close', {
            duration: 5000,
            panelClass: ['success-snackbar']
          });
          
          // Reload balance
          this.loadBalance();
          
          // Reset form
          this.transferForm.reset();
        },
        error: (error: { message: string; }) => {
          this.isLoading = false;
          const errorMessage = error.message || 'Transfer failed. Please try again.';
          this.snackBar.open(errorMessage, 'Close', {
            duration: 5000,
            panelClass: ['error-snackbar']
          });
        }
      });
    }
  }

  resetForm(): void {
    this.transferForm.reset();
    this.transferSuccess = false;
    this.transferResult = null;
  }

  viewHistory(): void {
    this.router.navigate(['/history']);
  }

  backToDashboard(): void {
    this.router.navigate(['/dashboard']);
  }
}