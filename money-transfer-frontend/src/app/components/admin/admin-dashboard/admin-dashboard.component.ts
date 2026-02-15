// src/app/components/admin/admin-dashboard/admin-dashboard.component.ts

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatBadgeModule } from '@angular/material/badge';
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
    MatBadgeModule,
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
  pendingUsers = 0;

  loadStatistics(): void {
    this.userManagementService.getAllUsers().subscribe({
      next: (users) => {
        this.totalUsers = users.filter(u => u.role != 'ADMIN').length;;
        this.activeUsers = users.filter(u => u.status === 'ACTIVE' && u.role != 'ADMIN').length;
        this.inactiveUsers = users.filter(u => u.status === 'INACTIVE' && u.role != 'ADMIN').length;
        this.pendingUsers = users.filter(u => u.status === 'PENDING' && u.role != 'ADMIN').length; // ← ADD THIS

      }
    });


  }

  navigateToUserList(): void {
    this.router.navigate(['/admin/users']);
  }

  navigateToCreateUser(): void {
    this.router.navigate(['/admin/create-user']);
  }


}