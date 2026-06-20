import { Routes } from '@angular/router';
import { LoginComponent } from './components/login/login.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { TransferComponent } from './components/transfer/transfer.component';
import { HistoryComponent } from './components/history/history.component';
import { AdminDashboardComponent } from './components/admin/admin-dashboard/admin-dashboard.component';
import { AdminLoginComponent } from './components/admin/admin-login/admin-login.component';
import { UserListComponent } from './components/admin/user-list/user-list.component';
import { authGuard } from './guards/auth.guard';
import { adminGuard } from './guards/admin.guard';
import { userGuard } from './guards/user.guard';
import { SignupComponent } from './components/signup/signup.component';
import { ForgotPasswordComponent } from './components/forgot-password/forgot-password.component';
import { RewardsComponent } from './components/rewards/rewards.component';

export const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'signup', component: SignupComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },

  // Separate admin login (hidden from regular users)
  { path: 'admin/login', component: AdminLoginComponent },

  // User routes
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [authGuard, userGuard]
  },
  {
    path: 'transfer',
    component: TransferComponent,
    canActivate: [authGuard, userGuard]
  },
  {
    path: 'history',
    component: HistoryComponent,
    canActivate: [authGuard, userGuard]
  },
  {
    path: 'rewards',
    component: RewardsComponent,
    canActivate: [authGuard, userGuard]
  },

  // Admin routes
  {
    path: 'admin',
    component: AdminDashboardComponent,
    canActivate: [authGuard, adminGuard]
  },
  {
    path: 'admin/users',
    component: UserListComponent,
    canActivate: [authGuard, adminGuard]
  },

  { path: '**', redirectTo: '/login' }
];
