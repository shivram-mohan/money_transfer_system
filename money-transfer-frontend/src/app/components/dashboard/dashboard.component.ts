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
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AuthService } from '../../services/auth.service';
import { AccountService } from '../../services/account.service';
import { CryptoService } from '../../services/crypto.service';
import { NavbarComponent } from '../navbar/navbar.component';
import { PasswordPromptComponent } from '../password-prompt/password-prompt.component';

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
    MatDialogModule,
    MatTooltipModule,
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

  // The balance arrives AES-encrypted and is only decrypted into `balance` when
  // the user reveals it, so the plaintext value is never held before then.
  private encryptedBalance: string | null = null;

  // Balance is hidden behind a password prompt until the user reveals it
  balanceVisible = false;

  // Bank linking
  isBankLinked = false;
  isLinking = false;
  linkForm: FormGroup;

  constructor(
    private authService: AuthService,
    private accountService: AccountService,
    private cryptoService: CryptoService,
    private router: Router,
    private fb: FormBuilder,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
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
    } else {
      this.isLoading = false;
    }
  }

  loadBalance(): void {
    if (this.accountId) {
      this.isLoading = true;
      this.accountService.getBalance(this.accountId).subscribe({
        next: (encrypted: string) => {
          // Keep the ciphertext; decrypt lazily only when the user reveals it.
          this.encryptedBalance = encrypted;
          // If the balance is currently shown (e.g. a refresh), update it.
          if (this.balanceVisible) {
            this.revealBalance();
          }
          this.isLoading = false;
        },
        error: (error: any) => {
          console.error('Error loading balance:', error);
          this.isLoading = false;
        }
      });
    }
  }

  private async revealBalance(): Promise<void> {
    if (!this.encryptedBalance) {
      return;
    }
    try {
      this.balance = await this.cryptoService.decryptToNumber(this.encryptedBalance);
      this.balanceVisible = true;
    } catch (e) {
      console.error('Error decrypting balance:', e);
      this.snackBar.open('Unable to display balance', 'Close', { duration: 4000 });
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
        // Store the encrypted balance; it's revealed (decrypted) on demand.
        this.encryptedBalance = response.encryptedBalance;
        this.balanceVisible = false;
        this.isBankLinked = true;
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

  // ─── BALANCE VISIBILITY ──────────────────────────────────────────

  toggleBalanceVisibility(): void {
    if (this.balanceVisible) {
      this.balanceVisible = false;
      return;
    }
    this.promptForBalanceReveal();
  }

  private promptForBalanceReveal(): void {
    const dialogRef = this.dialog.open(PasswordPromptComponent, {
      width: '400px',
      disableClose: false,
      autoFocus: true
    });

    dialogRef.afterClosed().subscribe((verified: boolean) => {
      if (verified) {
        // Decrypt the balance only now that the user has re-verified.
        this.revealBalance();
      }
    });
  }
}
