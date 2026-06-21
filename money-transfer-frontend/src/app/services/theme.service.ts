// src/app/services/theme.service.ts

import { Injectable, PLATFORM_ID, inject, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

export type AppTheme = 'light' | 'dark';

/**
 * Tracks and applies the app-wide colour theme. The default is LIGHT (black text
 * on silver-white); toggling adds/removes the `dark-theme` class on <html>, which
 * styles.scss keys off to flip every --aurora-* variable and the Material colors.
 * The choice is persisted so it survives reloads. SSR-safe (no-ops on the server).
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly STORAGE_KEY = 'app-theme';
  private readonly DARK_CLASS = 'dark-theme';

  private platformId = inject(PLATFORM_ID);
  private isBrowser = isPlatformBrowser(this.platformId);

  /** Current theme, readable in templates (e.g. `themeService.theme()`). */
  readonly theme = signal<AppTheme>('light');

  /** Read the persisted choice and apply it. Call once at app start. */
  init(): void {
    if (!this.isBrowser) {
      return;
    }
    const saved = localStorage.getItem(this.STORAGE_KEY) as AppTheme | null;
    this.apply(saved === 'dark' ? 'dark' : 'light');
  }

  /** Flip between light and dark. */
  toggle(): void {
    this.apply(this.theme() === 'dark' ? 'light' : 'dark');
  }

  setTheme(theme: AppTheme): void {
    this.apply(theme);
  }

  isDark(): boolean {
    return this.theme() === 'dark';
  }

  private apply(theme: AppTheme): void {
    this.theme.set(theme);
    if (!this.isBrowser) {
      return;
    }
    document.documentElement.classList.toggle(this.DARK_CLASS, theme === 'dark');
    localStorage.setItem(this.STORAGE_KEY, theme);
  }
}
