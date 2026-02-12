// src/app/app.routes.ts

import { Routes } from '@angular/router';
import { LoginComponent } from './components/login/login.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { TransferComponent } from './components/transfer/transfer.component';
import { HistoryComponent } from './components/history/history.component';
import { AdminDashboardComponent } from './components/admin/admin-dashboard/admin-dashboard.component';
import { CreateUserComponent } from './components/admin/create-user/create-user.component';
import { UserListComponent } from './components/admin/user-list/user-list.component';
import { authGuard } from './guards/auth.guard';


export const routes: Routes = [
  // Default redirect
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  
  // Login page (no guard)
  { path: 'login', component: LoginComponent },
  
  // User routes (protected by authGuard)
  { 
    path: 'dashboard', 
    component: DashboardComponent, 
    canActivate: [authGuard] 
  },
  { 
    path: 'transfer', 
    component: TransferComponent, 
    canActivate: [authGuard] 
  },
  { 
    path: 'history', 
    component: HistoryComponent, 
    canActivate: [authGuard] 
  },
  
  // Admin routes (protected by adminGuard)
  { 
    path: 'admin/dashboard', 
    component: AdminDashboardComponent, 
    canActivate: [authGuard] 
  },
  { 
    path: 'admin/create-user', 
    component: CreateUserComponent, 
    canActivate: [authGuard] 
  },
  { 
    path: 'admin/users', 
    component: UserListComponent, 
    canActivate: [authGuard] 
  },

  { path: '**', redirectTo: '/login' }
];