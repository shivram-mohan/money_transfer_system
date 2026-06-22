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
import { CryptoService } from '../../services/crypto.service';
import { NavbarComponent } from '../navbar/navbar.component';
import { TransferRequest } from '../../models/transaction.model';
import { ConfirmDialogComponent } from '../admin/confirm-dialog/confirm-dialog.component';

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
    private cryptoService: CryptoService,
    private router: Router,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {
    this.transferForm = this.fb.group({
      toAccountId: ['', [Validators.required, Validators.min(1)]],
      amount: ['', [Validators.required, Validators.min(0.01)]]
    });
  }

  ngOnInit(): void {
    // Block access until the user has linked a bank account
    if (!this.authService.isBankLinked()) {
      this.snackBar.open(
        'Link a bank account to start transferring money',
        'Close',
        { duration: 4000 }
      );
      this.router.navigate(['/dashboard']);
      return;
    }

    this.currentAccountId = this.authService.getCurrentAccountId();
    this.holderName = this.authService.getHolderName();

    if (this.currentAccountId) {
      this.loadBalance();
    }
  }

  loadBalance(): void {
    if (this.currentAccountId) {
      this.accountService.getBalance(this.currentAccountId).subscribe({
        next: (encrypted: string) => {
          // Balance arrives AES-encrypted; decrypt it for the available-balance display.
          this.cryptoService.decryptToNumber(encrypted)
            .then((value) => (this.currentBalance = value))
            .catch((e) => console.error('Error decrypting balance:', e));
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

      // NOTE: We intentionally do NOT block an over-balance transfer here.
      // The backend is the source of truth: it rejects the transfer AND records
      // it as a FAILED transaction in the history (e.g. insufficient balance),
      // which is exactly what we want the user to be able to review later.

      // Look up the receiver so the user can confirm WHO they're paying
      // before any money moves.
      this.isLoading = true;
      this.accountService.getAccount(toAccountId).subscribe({
        next: (account) => {
          this.isLoading = false;
          this.confirmAndTransfer(toAccountId, amount, account.holderName);
        },
        error: () => {
          this.isLoading = false;
          this.snackBar.open(
            `No account found with ID ${toAccountId}. Please check and try again.`,
            'Close',
            { duration: 5000, panelClass: ['error-snackbar'] }
          );
        }
      });
    }
  }

  // Show a confirmation dialog with the receiver's name, then transfer on confirm.
  private confirmAndTransfer(toAccountId: number, amount: number, receiverName: string): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '420px',
      data: {
        title: 'Confirm Transfer',
        message: `You are about to send ₹${amount.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} to ${receiverName} (Account #${toAccountId}). Do you want to proceed?`,
        confirmText: 'Send Money',
        cancelText: 'Cancel',
        icon: 'send',
        color: 'primary'
      }
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.executeTransfer(toAccountId, amount);
      }
    });
  }

  private executeTransfer(toAccountId: number, amount: number): void {
    if (!this.currentAccountId) {
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
  next: (response) => {
    this.isLoading = false;
    this.transferSuccess = true;
    this.transferResult = response;

    // Nudge the user to redeem when this transfer unlocked the threshold
    if (response.reward?.reachedRedeemThreshold) {
      this.snackBar.open(
        '🎉 You\'ve reached 500 points — you can now redeem for cash!',
        'Redeem',
        { duration: 8000, panelClass: ['success-snackbar'] }
      ).onAction().subscribe(() => this.router.navigate(['/rewards']));
    }

    // ✅ Show recipient name if available
    this.accountService.getAccount(response.creditedTo).subscribe({
      next: (account) => {
        this.snackBar.open(
          `₹${response.amount} sent successfully to ${account.holderName}!`, 
          'Close', 
          {
            duration: 5000,
            panelClass: ['success-snackbar']
          }
        );
      },
      error: () => {
        // Fallback if account fetch fails
        this.snackBar.open(
          'Transfer completed successfully!', 
          'Close', 
          {
            duration: 5000,
            panelClass: ['success-snackbar']
          }
        );
      }
    });
    
    this.loadBalance();
    this.transferForm.reset();
  },
  error: (error) => {
    this.isLoading = false;
    const errorMsg = error.error?.message || 'Transfer failed. Please try again.';
    this.snackBar.open(errorMsg, 'Close', {
      duration: 5000,
      panelClass: ['error-snackbar']
    });
  }
});
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