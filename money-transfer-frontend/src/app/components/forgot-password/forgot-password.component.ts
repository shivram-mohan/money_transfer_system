// src/app/components/forgot-password/forgot-password.component.ts

import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';
import { MatDivider } from '@angular/material/divider';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [
    RouterModule,
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatIconModule,
    MatDivider
  ],
  templateUrl: './forgot-password.component.html',
  styleUrls: ['./forgot-password.component.scss']
})
export class ForgotPasswordComponent {
  usernameForm: FormGroup;
  resetForm: FormGroup;

  isLoading = false;
  hidePassword = true;
  hideConfirmPassword = true;
  currentStep = 1; // 1: enter username, 2: OTP + new password
  maskedEmail = '';

  private username = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {
    this.usernameForm = this.fb.group({
      username: ['', [Validators.required]]
    });

    this.resetForm = this.fb.group({
      otp: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(6)]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required]]
    });
  }

  // Step 1: Send reset OTP to the registered email
  onRequestOtp(): void {
    if (this.usernameForm.valid) {
      this.isLoading = true;
      this.username = this.usernameForm.value.username;

      this.authService.forgotPassword({ username: this.username }).subscribe({
        next: (response) => {
          this.isLoading = false;
          this.maskedEmail = response.email;
          this.currentStep = 2;
          this.snackBar.open(
            `OTP sent to ${response.email}`,
            'Close',
            { duration: 5000, panelClass: ['success-snackbar'] }
          );
        },
        error: (error) => {
          this.isLoading = false;
          this.snackBar.open(
            error.error?.message || 'Could not start password reset',
            'Close',
            { duration: 5000, panelClass: ['error-snackbar'] }
          );
        }
      });
    }
  }

  // Step 2: Verify OTP and set the new password
  onResetPassword(): void {
    if (this.resetForm.valid) {
      if (this.resetForm.value.password !== this.resetForm.value.confirmPassword) {
        this.snackBar.open('Passwords do not match', 'Close', {
          duration: 3000, panelClass: ['error-snackbar']
        });
        return;
      }

      this.isLoading = true;

      this.authService.resetPassword({
        username: this.username,
        otp: this.resetForm.value.otp,
        password: this.resetForm.value.password
      }).subscribe({
        next: () => {
          this.isLoading = false;
          this.snackBar.open(
            'Password reset successfully! You can now log in.',
            'Close',
            { duration: 5000, panelClass: ['success-snackbar'] }
          );
          setTimeout(() => this.router.navigate(['/login']), 1500);
        },
        error: (error) => {
          this.isLoading = false;
          this.snackBar.open(
            error.error?.message || 'Password reset failed',
            'Close',
            { duration: 5000, panelClass: ['error-snackbar'] }
          );
        }
      });
    }
  }

  resendOtp(): void {
    this.isLoading = true;
    this.authService.forgotPassword({ username: this.username }).subscribe({
      next: (response) => {
        this.isLoading = false;
        this.snackBar.open(
          `New OTP sent to ${response.email}`,
          'Close',
          { duration: 3000, panelClass: ['success-snackbar'] }
        );
      },
      error: (error) => {
        this.isLoading = false;
        this.snackBar.open(
          error.error?.message || 'Failed to resend OTP',
          'Close',
          { duration: 3000, panelClass: ['error-snackbar'] }
        );
      }
    });
  }

  goBack(): void {
    this.currentStep = 1;
    this.resetForm.reset();
  }

  togglePasswordVisibility(): void {
    this.hidePassword = !this.hidePassword;
  }

  toggleConfirmPasswordVisibility(): void {
    this.hideConfirmPassword = !this.hideConfirmPassword;
  }
}
