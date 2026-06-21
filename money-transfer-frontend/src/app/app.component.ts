// src/app/app.component.ts

import { Component, OnDestroy, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subscription } from 'rxjs';
import { AuthService } from './services/auth.service';
import { InactivityService } from './services/inactivity.service';
import { ThemeService } from './services/theme.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, MatButtonModule, MatIconModule, MatTooltipModule],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit, OnDestroy {
  title = 'Money Transfer System';

  private authSub?: Subscription;

  constructor(
    private authService: AuthService,
    private inactivityService: InactivityService,
    // public so the template can read the current theme for the toggle icon
    public themeService: ThemeService
  ) {}

  toggleTheme(): void {
    this.themeService.toggle();
  }

  ngOnInit(): void {
    // Apply the persisted theme (defaults to light) as early as possible.
    this.themeService.init();

    // Run the inactivity watchdog only while a session is active.
    this.authSub = this.authService.isAuthenticated$.subscribe(
      (isAuthenticated) => {
        if (isAuthenticated) {
          this.inactivityService.start();
        } else {
          this.inactivityService.stop();
        }
      }
    );
  }

  ngOnDestroy(): void {
    this.authSub?.unsubscribe();
    this.inactivityService.stop();
  }
}
