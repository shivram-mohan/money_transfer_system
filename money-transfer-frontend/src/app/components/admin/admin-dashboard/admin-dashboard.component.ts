// src/app/components/admin/admin-dashboard/admin-dashboard.component.ts

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../services/auth.service';
import { UserManagementService } from '../../../services/user-management.service';
import { AdminNavbarComponent } from '../admin-navbar/admin-navbar.component';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    AdminNavbarComponent
  ],
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.scss']
})
export class AdminDashboardComponent implements OnInit {
  adminName: string | null = null;
  totalUsers = 0;
  activeUsers = 0;
  inactiveUsers = 0;

  constructor(
    private authService: AuthService,
    private userManagementService: UserManagementService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.adminName = this.authService.getHolderName();
    this.loadStatistics();
  }

  loadStatistics(): void {
    this.userManagementService.getAllUsers().subscribe({
      next: (users) => {
        const nonAdminUsers = users.filter(u => u.role != 'ADMIN');
        this.totalUsers = nonAdminUsers.length;
        this.activeUsers = nonAdminUsers.filter(u => u.status === 'ACTIVE').length;
        this.inactiveUsers = nonAdminUsers.filter(u => u.status === 'INACTIVE').length;
      }
    });
  }

  navigateToUserList(): void {
    this.router.navigate(['/admin/users']);
  }
}
