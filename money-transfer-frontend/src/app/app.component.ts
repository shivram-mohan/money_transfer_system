// src/app/app.component.ts

import { Component, OnDestroy, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService } from './services/auth.service';
import { InactivityService } from './services/inactivity.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit, OnDestroy {
  title = 'Money Transfer System';

  private authSub?: Subscription;

  constructor(
    private authService: AuthService,
    private inactivityService: InactivityService
  ) {}

  ngOnInit(): void {
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
