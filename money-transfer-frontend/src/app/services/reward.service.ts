// src/app/services/reward.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { RewardProfile, RedeemResponse } from '../models/reward.model';

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

  /** Redeem reward points as cash into the user's bank account. */
  redeem(points: number): Observable<RedeemResponse> {
    return this.http.post<RedeemResponse>(`${this.apiUrl}/rewards/redeem`, { points });
  }
}
