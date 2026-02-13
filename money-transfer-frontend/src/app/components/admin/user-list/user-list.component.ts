// src/app/components/admin/user-list/user-list.component.ts

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { UserManagementService } from '../../../services/user-management.service';
import { UserResponse, UserStatus } from '../../../models/user.model';
import { AdminNavbarComponent } from '../admin-navbar/admin-navbar.component';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTooltipModule,
    AdminNavbarComponent
  ],
  templateUrl: './user-list.component.html',
  styleUrls: ['./user-list.component.scss']
})
export class UserListComponent implements OnInit {
  users: UserResponse[] = [];
  displayedColumns: string[] = ['id', 'username', 'name', 'accountId', 'status', 'actions'];
  isLoading = true;

  constructor(
    private userManagementService: UserManagementService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.isLoading = true;
    this.userManagementService.getAllUsers().subscribe({
      next: (users) => {
        this.users = users;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading users:', error);
        this.isLoading = false;
        this.snackBar.open('Failed to load users', 'Close', {
          duration: 3000,
          panelClass: ['error-snackbar']
        });
      }
    });
  }

  // Helper methods for counting
  get activeUsersCount(): number {
    return this.users.filter(u => u.status === UserStatus.ACTIVE).length;
  }

  get pendingUsersCount(): number {
    return this.users.filter(u => u.status === UserStatus.PENDING).length;
  }

  get inactiveUsersCount(): number {
    return this.users.filter(u => u.status === UserStatus.INACTIVE).length;
  }

  getStatusClass(status: UserStatus): string {
    switch (status) {
      case UserStatus.ACTIVE:
        return 'status-active';
      case UserStatus.INACTIVE:
        return 'status-inactive';
      case UserStatus.PENDING:
        return 'status-pending';
      case UserStatus.LOCKED:
        return 'status-locked';
      default:
        return '';
    }
  }

  deactivateUser(userId: number, username: string): void {
    if (confirm(`Are you sure you want to deactivate user "${username}"?`)) {
      this.userManagementService.deactivateUser({
        userId,
        reason: 'Deactivated by admin'
      }).subscribe({
        next: () => {
          this.snackBar.open('User deactivated successfully', 'Close', {
            duration: 3000,
            panelClass: ['success-snackbar']
          });
          this.loadUsers();
        },
        error: (error) => {
          this.snackBar.open(error.message || 'Failed to deactivate user', 'Close', {
            duration: 3000,
            panelClass: ['error-snackbar']
          });
        }
      });
    }
  }

  activateUser(userId: number, username: string): void {
    if (confirm(`Are you sure you want to activate user "${username}"?`)) {
      this.userManagementService.activateUser(userId).subscribe({
        next: () => {
          this.snackBar.open('User activated successfully', 'Close', {
            duration: 3000,
            panelClass: ['success-snackbar']
          });
          this.loadUsers();
        },
        error: (error) => {
          this.snackBar.open(error.message || 'Failed to activate user', 'Close', {
            duration: 3000,
            panelClass: ['error-snackbar']
          });
        }
      });
    }
  }

  formatDate(date: Date): string {
    return new Date(date).toLocaleDateString('en-IN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }

  refreshList(): void {
    this.loadUsers();
  }
}