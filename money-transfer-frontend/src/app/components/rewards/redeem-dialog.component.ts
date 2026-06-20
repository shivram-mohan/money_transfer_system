// src/app/components/rewards/redeem-dialog.component.ts

import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import {
  MatDialogModule,
  MatDialogRef,
  MAT_DIALOG_DATA
} from '@angular/material/dialog';

export interface RedeemDialogData {
  balance: number;
  threshold: number;
}

@Component({
  selector: 'app-redeem-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatDialogModule
  ],
  template: `
    <h2 mat-dialog-title class="redeem-title">
      <mat-icon>redeem</mat-icon> Redeem points for cash
    </h2>
    <mat-dialog-content>
      <p class="redeem-sub">
        1 point = ₹1 · You have <strong>{{ data.balance | number }}</strong> points
        (min {{ data.threshold }} to redeem).
      </p>
      <form [formGroup]="form">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Points to redeem</mat-label>
          <input matInput type="number" formControlName="points"
                 [min]="data.threshold" [max]="data.balance" />
          <span matTextSuffix>= ₹{{ form.value.points || 0 }}</span>
          <mat-error *ngIf="form.get('points')?.hasError('required')">
            Enter how many points to redeem
          </mat-error>
          <mat-error *ngIf="form.get('points')?.hasError('min')">
            Minimum is {{ data.threshold }} points
          </mat-error>
          <mat-error *ngIf="form.get('points')?.hasError('max')">
            You only have {{ data.balance | number }} points
          </mat-error>
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="cancel()">Cancel</button>
      <button mat-raised-button color="primary" [disabled]="form.invalid" (click)="confirm()">
        <mat-icon>payments</mat-icon> Redeem
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .redeem-title { display: flex; align-items: center; gap: 8px; }
    .redeem-title mat-icon { color: #ffd700; }
    .redeem-sub { color: var(--aurora-text-secondary); margin-bottom: 16px; }
    .full-width { width: 100%; }
  `]
})
export class RedeemDialogComponent {
  form: FormGroup;

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<RedeemDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: RedeemDialogData
  ) {
    this.form = this.fb.group({
      points: [
        data.balance,
        [
          Validators.required,
          Validators.min(data.threshold),
          Validators.max(data.balance)
        ]
      ]
    });
  }

  cancel(): void {
    this.dialogRef.close();
  }

  confirm(): void {
    if (this.form.valid) {
      this.dialogRef.close(Number(this.form.value.points));
    }
  }
}
