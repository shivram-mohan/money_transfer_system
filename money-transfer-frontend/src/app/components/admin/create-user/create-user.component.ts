import { Component } from '@angular/core';
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
import { AccountService, CreateAccountRequest } from '../../../services/account.service';
import { AdminNavbarComponent } from '../admin-navbar/admin-navbar.component';

@Component({
  selector: 'app-create-user',
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
    AdminNavbarComponent
  ],
  templateUrl: './create-user.component.html',
  styleUrls: ['./create-user.component.scss']
})
export class CreateUserComponent {
  createUserForm: FormGroup;
  isLoading = false;

  constructor(
    private fb: FormBuilder,
    private accountService: AccountService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {
    this.createUserForm = this.fb.group({
      name: ['', [Validators.required]],
      username: ['', [Validators.required]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      initialBalance: [1000, [Validators.required, Validators.min(0)]]
    });
  }

  onSubmit(): void {
    if (this.createUserForm.valid) {
      this.isLoading = true;

      const request: CreateAccountRequest = {
        holderName: this.createUserForm.value.name,
        username: this.createUserForm.value.username,
        password: this.createUserForm.value.password,
        initialBalance: this.createUserForm.value.initialBalance
      };

      this.accountService.createAccount(request).subscribe({
        next: (response) => {
          this.isLoading = false;
          this.snackBar.open(
            `Account created! ID: ${response.id}, Username: ${response.username}`,
            'Close',
            {
              duration: 5000,
              panelClass: ['success-snackbar']
            }
          );
          this.createUserForm.reset({ initialBalance: 1000 });
          this.router.navigate(['/admin/dashboard']);
        },
        error: (error) => {
          this.isLoading = false;
          this.snackBar.open(
            error?.error?.message || 'Failed to create account',
            'Close',
            {
              duration: 5000,
              panelClass: ['error-snackbar']
            }
          );
        }
      });
    }
  }

  cancel(): void {
    this.router.navigate(['/admin/dashboard']);
  }
}
