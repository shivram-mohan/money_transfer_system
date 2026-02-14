import { Routes } from '@angular/router';
import { LoginComponent } from './components/login/login.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { TransferComponent } from './components/transfer/transfer.component';
import { HistoryComponent } from './components/history/history.component';
import { AdminDashboardComponent } from './components/admin/admin-dashboard/admin-dashboard.component';
import { UserListComponent } from './components/admin/user-list/user-list.component';
import { CreateUserComponent } from './components/admin/create-user/create-user.component';
import { authGuard } from './guards/auth.guard';
import { adminGuard } from './guards/admin.guard'; // ← Make sure this is correct
import { SignupComponent } from './components/signup/signup.component';

export const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'signup', component: SignupComponent }, // ← ADD THIS

  // User routes
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

  // Admin routes  
  { 
    path: 'admin', 
    component: AdminDashboardComponent, 
    canActivate: [authGuard, adminGuard]  // ← Must be functions not arrays
  },
  { 
    path: 'admin/users', 
    component: UserListComponent, 
    canActivate: [authGuard, adminGuard] 
  },
  { 
    path: 'admin/create-user', 
    component: CreateUserComponent, 
    canActivate: [authGuard, adminGuard] 
  },

  { path: '**', redirectTo: '/login' }
];