// src/app/services/reward.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { RewardProfile, MonthlySummary } from '../models/reward.model';

@Injectable({
  providedIn: 'root'
})
export class RewardService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  /** Current user's rewards standing. */
  getMyRewards(): Observable<RewardProfile> {
    return this.http.get<RewardProfile>(`${this.apiUrl}/rewards/me`);
  }

  /** Reward summary for a month (YYYY-MM); defaults to current month. */
  getMonthlySummary(month?: string): Observable<MonthlySummary> {
    let params = new HttpParams();
    if (month) {
      params = params.set('month', month);
    }
    return this.http.get<MonthlySummary>(`${this.apiUrl}/rewards/me/summary`, { params });
  }
}
