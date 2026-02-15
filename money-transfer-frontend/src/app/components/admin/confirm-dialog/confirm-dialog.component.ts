import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

export interface ConfirmDialogData {
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  icon?: string;
  color?: 'warn' | 'primary' | 'accent';
}

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <div class="confirm-dialog">
      <div class="dialog-header">
        <div class="icon-wrapper" [class]="data.color || 'warn'">
          <mat-icon>{{ data.icon || 'warning' }}</mat-icon>
        </div>
        <h2 mat-dialog-title>{{ data.title }}</h2>
      </div>
      <mat-dialog-content>
        <p>{{ data.message }}</p>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-stroked-button (click)="onCancel()">
          {{ data.cancelText || 'Cancel' }}
        </button>
        <button mat-flat-button [color]="data.color || 'warn'" (click)="onConfirm()">
          <mat-icon>{{ data.icon || 'warning' }}</mat-icon>
          {{ data.confirmText || 'Confirm' }}
        </button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [`
    .confirm-dialog {
      padding: 8px;
    }

    .dialog-header {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 12px;
      margin-bottom: 8px;
    }

    .icon-wrapper {
      width: 56px;
      height: 56px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;

      mat-icon {
        font-size: 28px;
        height: 28px;
        width: 28px;
        color: white;
      }

      &.warn {
        background: linear-gradient(135deg, #f87171 0%, #ec4899 100%);
      }

      &.primary {
        background: linear-gradient(135deg, #06b6d4 0%, #8b5cf6 100%);
      }

      &.accent {
        background: linear-gradient(135deg, #10b981 0%, #06b6d4 100%);
      }
    }

    h2 {
      text-align: center;
      margin: 0;
      font-size: 20px;
      font-weight: 600;
    }

    mat-dialog-content p {
      text-align: center;
      font-size: 14px;
      color: var(--aurora-text-secondary, #94a3b8);
      line-height: 1.6;
      margin: 0;
    }

    mat-dialog-actions {
      padding-top: 16px;
      gap: 12px;

      button {
        min-width: 100px;
      }
    }
  `]
})
export class ConfirmDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<ConfirmDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ConfirmDialogData
  ) {}

  onCancel(): void {
    this.dialogRef.close(false);
  }

  onConfirm(): void {
    this.dialogRef.close(true);
  }
}
