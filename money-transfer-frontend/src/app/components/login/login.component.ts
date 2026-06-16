// src/app/components/login/login.component.ts

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
  selector: 'app-login',
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
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent {
  loginForm: FormGroup;
  otpForm: FormGroup;

  isLoading = false;
  hidePassword = true;
  currentStep = 1; // 1: credentials, 2: OTP verification
  maskedEmail = '';

  // Store credentials for step 2
  private username = '';
  private password = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {
    this.loginForm = this.fb.group({
      username: ['', [Validators.required]],
      password: ['', [Validators.required]]
    });

    this.otpForm = this.fb.group({
      otp: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(6)]]
    });
  }

  // Step 1: Validate credentials and send OTP
  onLogin(): void {
    if (this.loginForm.valid) {
      this.isLoading = true;
      this.username = this.loginForm.value.username;
      this.password = this.loginForm.value.password;

      this.authService.loginStep1({
        username: this.username,
        password: this.password
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
            error.error?.message || 'Invalid credentials',
            'Close',
            { duration: 5000, panelClass: ['error-snackbar'] }
          );
        }
      });
    }
  }

  // Step 2: Verify OTP and get JWT
  onVerifyOtp(): void {
    if (this.otpForm.valid) {
      this.isLoading = true;

      this.authService.loginVerifyOtp({
        username: this.username,
        password: this.password,
        otp: this.otpForm.value.otp
      }).subscribe({
        next: (response) => {
          this.isLoading = false;
          this.snackBar.open(
            `Welcome, ${response.holderName}!`,
            'Close',
            { duration: 3000 }
          );
          this.router.navigate(['/dashboard']);
        },
        error: (error) => {
          this.isLoading = false;
          this.snackBar.open(
            error.error?.message || 'OTP verification failed',
            'Close',
            { duration: 5000, panelClass: ['error-snackbar'] }
          );
        }
      });
    }
  }

  // Resend OTP
  resendOtp(): void {
    this.isLoading = true;
    this.authService.loginStep1({
      username: this.username,
      password: this.password
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

  // Go back to credentials step
  goBack(): void {
    this.currentStep = 1;
    this.otpForm.reset();
  }

  togglePasswordVisibility(): void {
    this.hidePassword = !this.hidePassword;
  }
}
