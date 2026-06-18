// src/app/services/reward.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { RewardResponse } from '../models/reward.model';

@Injectable({
  providedIn: 'root'
})
export class RewardService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getRewards(accountId: number): Observable<RewardResponse> {
    return this.http.get<RewardResponse>(
      `${this.apiUrl}/rewards/${accountId}`
    );
  }
}
