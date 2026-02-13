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
    
    this.authService.login({
      ...this.userLoginForm.value,
      isAdmin: false
    }).subscribe({
      next: (response) => {
        this.isUserLoading = false;
        this.snackBar.open(
          `Welcome, ${response.holderName}!`, 
          'Close', 
          { duration: 3000 }
        );
        this.router.navigate(['/dashboard']);
      },
      error: (error) => {
        this.isUserLoading = false;
        this.snackBar.open(
          error.error?.message || 'Invalid credentials', 
          'Close',
          { duration: 5000, panelClass: ['error-snackbar'] }
        );
      }
    });
  }
}

onAdminLogin(): void {
  if (this.adminLoginForm.valid) {
    this.isAdminLoading = true;
    
    this.authService.login({
      ...this.adminLoginForm.value,
      isAdmin: true
    }).subscribe({
      next: (response) => {
        this.isAdminLoading = false;
        
        if (response.role === UserRole.ADMIN) {
          this.snackBar.open(
            `Welcome, ${response.holderName}!`,
            'Close',
            { duration: 3000 }
          );
          this.router.navigate(['/admin']);  // ← '/admin' not '/admin/dashboard'
        } else {
          this.snackBar.open('Admin access denied', 'Close', {
            duration: 3000,
            panelClass: ['error-snackbar']
          });
        }
      },
      error: (error) => {
        this.isAdminLoading = false;
        this.snackBar.open(
          error.error?.message || 'Invalid credentials',
          'Close',
          { duration: 5000, panelClass: ['error-snackbar'] }
        );
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