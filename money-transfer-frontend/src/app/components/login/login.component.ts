// src/app/components/login/login.component.ts

import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { AuthService } from '../../services/auth.service';
import { UserRole } from '../../models/user.model';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatIconModule,
    MatTabsModule
  ],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent {
  userLoginForm: FormGroup;
  adminLoginForm: FormGroup;
  isUserLoading = false;
  isAdminLoading = false;
  hideUserPassword = true;
  hideAdminPassword = true;
  selectedTabIndex = 0;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {
    this.userLoginForm = this.fb.group({
      username: ['', [Validators.required]],
      password: ['', [Validators.required]]
    });

    this.adminLoginForm = this.fb.group({
      username: ['', [Validators.required]],
      password: ['', [Validators.required]]
    });
  }

  onUserLogin(): void {
    if (this.userLoginForm.valid) {
      this.isUserLoading = true;
      
      const loginRequest = {
        ...this.userLoginForm.value,
        isAdmin: false
      };
      
      console.log('User login request:', loginRequest); // Debug log
      
      this.authService.login(loginRequest).subscribe({
        next: (response) => {
          console.log('User login response:', response); // Debug log
          this.isUserLoading = false;
          this.snackBar.open(`Welcome, ${response.holderName}!`, 'Close', {
            duration: 3000,
            horizontalPosition: 'end',
            verticalPosition: 'top'
          });
          
          console.log('Navigating to user dashboard'); // Debug log
          this.router.navigate(['/dashboard']);
        },
        error: (error) => {
          console.error('User login error:', error); // Debug log
          this.isUserLoading = false;
          this.snackBar.open(error.message || 'Login failed. Please try again.', 'Close', {
            duration: 5000,
            horizontalPosition: 'end',
            verticalPosition: 'top',
            panelClass: ['error-snackbar']
          });
        }
      });
    }
  }

  onAdminLogin(): void {
    if (this.adminLoginForm.valid) {
      this.isAdminLoading = true;
      
      const loginRequest = {
        ...this.adminLoginForm.value,
        isAdmin: true
      };
      
      console.log('Admin login request:', loginRequest); // Debug log
      
      this.authService.login(loginRequest).subscribe({
        next: (response) => {
          console.log('Admin login response:', response); // Debug log
          console.log('Admin role:', response.role); // Debug log
          
          this.isAdminLoading = false;
          this.snackBar.open(`Welcome, ${response.holderName}!`, 'Close', {
            duration: 3000,
            horizontalPosition: 'end',
            verticalPosition: 'top'
          });
          
          // Verify admin role before navigation
          if (response.role === UserRole.ADMIN) {
            console.log('Role is ADMIN, navigating to admin dashboard'); // Debug log
            setTimeout(() => {
              this.router.navigate(['/admin/dashboard']);
            }, 100); // Small delay to ensure localStorage is set
          } else {
            console.error('Role is not ADMIN:', response.role); // Debug log
            this.snackBar.open('Admin access denied', 'Close', {
              duration: 3000,
              panelClass: ['error-snackbar']
            });
          }
        },
        error: (error) => {
          console.error('Admin login error:', error); // Debug log
          this.isAdminLoading = false;
          this.snackBar.open(error.message || 'Login failed. Please try again.', 'Close', {
            duration: 5000,
            horizontalPosition: 'end',
            verticalPosition: 'top',
            panelClass: ['error-snackbar']
          });
        }
      });
    }
  }

  toggleUserPasswordVisibility(): void {
    this.hideUserPassword = !this.hideUserPassword;
  }

  toggleAdminPasswordVisibility(): void {
    this.hideAdminPassword = !this.hideAdminPassword;
  }
}