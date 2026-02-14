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
import { AuthService } from '../../../services/auth.service';
import { UserManagementService } from '../../../services/user-management.service';
import { AdminNavbarComponent } from '../admin-navbar/admin-navbar.component';
import { MatOption } from "@angular/material/select";

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
    AdminNavbarComponent,
    MatOption
],
  templateUrl: './create-user.component.html',
  styleUrls: ['./create-user.component.scss']
})
export class CreateUserComponent {
  createUserForm: FormGroup;
  isLoading = false;
  hidePassword = true;
  accountTypes = [
  { value: 'SAVINGS', label: 'Savings Account' },
  { value: 'CURRENT', label: 'Current Account' }
];

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private userManagementService: UserManagementService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {
    this.createUserForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      name: ['', [Validators.required]],
      email: ['', [Validators.email]],
      initialBalance: [1000, [Validators.required, Validators.min(0)]],
      accountType: ['SAVINGS', Validators.required]
    });
  }

  ngOnInit(): void {
  this.createUserForm.get('accountType')?.valueChanges.subscribe(accountType => {
    const balanceControl = this.createUserForm.get('initialBalance');
    
    if (accountType === 'SAVINGS') {
      balanceControl?.setValidators([Validators.required, Validators.min(1000)]);
    } else {
      balanceControl?.setValidators([Validators.required, Validators.min(5000)]);
    }
    
    balanceControl?.updateValueAndValidity();
  });
}

  onSubmit(): void {
    if (this.createUserForm.valid) {
      this.isLoading = true;
      const adminName = this.authService.getHolderName() || 'admin';
      
      this.userManagementService.createUser(this.createUserForm.value, adminName).subscribe({
        next: (response) => {
          this.isLoading = false;
          this.snackBar.open('User created successfully!', 'Close', {
            duration: 5000,
            panelClass: ['success-snackbar']
          });
          this.createUserForm.reset({ initialBalance: 1000 });
          this.router.navigate(['/admin/dashboard']);
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

