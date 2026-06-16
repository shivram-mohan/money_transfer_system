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
import { UserManagementService } from '../../../services/user-management.service';
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
  hidePassword = true;

  constructor(
    private fb: FormBuilder,
    private userManagementService: UserManagementService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {
    this.createUserForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      name: ['', [Validators.required]],
      email: ['', [Validators.email]],
      initialBalance: [1000, [Validators.required, Validators.min(0)]]
    });
  }

  onSubmit(): void {
    if (this.createUserForm.valid) {
      this.isLoading = true;

      this.userManagementService.createUser(this.createUserForm.value).subscribe({
        next: (response) => {
          this.isLoading = false;
          this.snackBar.open('User created successfully!', 'Close', {
            duration: 5000,
            panelClass: ['success-snackbar']
          });
          this.createUserForm.reset({ initialBalance: 1000 });
          this.router.navigate(['/admin']);
        },
        error: (error) => {
          this.isLoading = false;
          this.snackBar.open(error.message || 'Failed to create user', 'Close', {
            duration: 5000,
            panelClass: ['error-snackbar']
          });
        }
      });
    }
  }

  cancel(): void {
    this.router.navigate(['/admin/dashboard']);
  }
}

