// src/app/components/password-prompt/password-prompt.component.ts

import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../services/auth.service';

/**
 * Modal that re-verifies the user's login password before a sensitive action.
 * The password is verified inside the dialog and never leaves it; the dialog
 * closes with `true` only when verification succeeds.
 */
@Component({
  selector: 'app-password-prompt',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './password-prompt.component.html',
  styleUrls: ['./password-prompt.component.scss']
})
export class PasswordPromptComponent {
  form: FormGroup;
  isVerifying = false;
  hidePassword = true;
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private dialogRef: MatDialogRef<PasswordPromptComponent, boolean>
  ) {
    this.form = this.fb.group({
      password: ['', [Validators.required]]
    });
  }

  confirm(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isVerifying = true;
    this.errorMessage = '';

    this.authService.verifyPassword(this.form.value.password).subscribe({
      next: () => {
        this.isVerifying = false;
        this.dialogRef.close(true);
      },
      error: (error) => {
        this.isVerifying = false;
        this.errorMessage = error.error?.message || 'Incorrect password';
      }
    });
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  togglePasswordVisibility(): void {
    this.hidePassword = !this.hidePassword;
  }
}
