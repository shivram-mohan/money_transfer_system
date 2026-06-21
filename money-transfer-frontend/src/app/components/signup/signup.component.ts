// src/app/components/signup/signup.component.ts

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
import { MatStepperModule } from '@angular/material/stepper';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatIconModule,
    MatStepperModule
  ],
  templateUrl: './signup.component.html',
  styleUrls: ['./signup.component.scss']
})
export class SignupComponent {
  // Step forms
  accountForm: FormGroup;
  otpForm: FormGroup;
  passwordForm: FormGroup;

  // State
  currentStep = 1; // 1: Account verify, 2: OTP verify, 3: Set password
  isLoading = false;
  hidePassword = true;
  hideConfirmPassword = true;
  maskedEmail = '';

  // Store data between steps
  private fullName: string = '';
  private username: string = '';
  private email: string = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {
    this.accountForm = this.fb.group({
      fullName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
      email: ['', [Validators.required, Validators.email]]
    });

    this.otpForm = this.fb.group({
      otp: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(6)]]
    });

    this.passwordForm = this.fb.group({
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required]]
    });
  }

  // Step 1: Verify account and send OTP
  onVerifyAccount(): void {
    if (this.accountForm.valid) {
      this.isLoading = true;

      this.fullName = this.accountForm.value.fullName;
      this.username = this.accountForm.value.username;
      this.email = this.accountForm.value.email;

      this.authService.verifyAccount({
        username: this.username,
        email: this.email
      }).subscribe({
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
            error.error?.message || 'Account verification failed',
            'Close',
            { duration: 5000, panelClass: ['error-snackbar'] }
          );
        }
      });
    } else {
      Object.keys(this.accountForm.controls).forEach(key => {
        this.accountForm.get(key)?.markAsTouched();
      });
    }
  }

  // Step 2: Verify OTP
  onVerifyOtp(): void {
    if (this.otpForm.valid) {
      this.isLoading = true;

      this.authService.verifySignupOtp({
        email: this.email,
        otp: this.otpForm.value.otp,
        purpose: 'SIGNUP'
      }).subscribe({
        next: () => {
          this.isLoading = false;
          this.currentStep = 3;
          this.snackBar.open(
            'OTP verified! Please set your password.',
            'Close',
            { duration: 3000, panelClass: ['success-snackbar'] }
          );
        },
        error: (error) => {
          this.isLoading = false;
          this.snackBar.open(
            error.error?.message || 'Invalid or expired OTP',
            'Close',
            { duration: 5000, panelClass: ['error-snackbar'] }
          );
        }
      });
    }
  }

  // Step 3: Set password and complete signup
  onSetPassword(): void {
    if (this.passwordForm.valid) {
      if (this.passwordForm.value.password !== this.passwordForm.value.confirmPassword) {
        this.snackBar.open('Passwords do not match', 'Close', {
          duration: 3000, panelClass: ['error-snackbar']
        });
        return;
      }

      this.isLoading = true;

      this.authService.setPassword({
        username: this.username,
        email: this.email,
        name: this.fullName,
        password: this.passwordForm.value.password
      }).subscribe({
        next: () => {
          this.isLoading = false;
          // Backend auto-logs-in on signup and the session is now persisted,
          // so take the user straight to their dashboard.
          this.snackBar.open(
            'Account created successfully! Welcome aboard.',
            'Close',
            { duration: 5000, panelClass: ['success-snackbar'] }
          );
          this.router.navigate(['/dashboard']);
        },
        error: (error) => {
          this.isLoading = false;
          this.snackBar.open(
            error.error?.message || 'Signup failed. Please try again.',
            'Close',
            { duration: 5000, panelClass: ['error-snackbar'] }
          );
        }
      });
    } else {
      Object.keys(this.passwordForm.controls).forEach(key => {
        this.passwordForm.get(key)?.markAsTouched();
      });
    }
  }

  // Resend OTP
  resendOtp(): void {
    this.isLoading = true;
    this.authService.verifyAccount({
      username: this.username,
      email: this.email
    }).subscribe({
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

  togglePasswordVisibility(): void {
    this.hidePassword = !this.hidePassword;
  }

  toggleConfirmPasswordVisibility(): void {
    this.hideConfirmPassword = !this.hideConfirmPassword;
  }
}
