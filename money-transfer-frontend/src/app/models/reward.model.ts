// src/app/models/reward.model.ts

/** Outcome of a single transfer's rewards (mirrors backend RewardResult). */
export interface RewardResult {
  rewarded: boolean;
  pointsEarned: number;
  weekendBonus: boolean;
  pointsBalance: number;
  lifetimePoints: number;
  reachedRedeemThreshold: boolean;
  message: string;
}

/** A user's current rewards standing (mirrors backend RewardProfileResponse). */
export interface RewardProfile {
  holderName: string;
  pointsBalance: number;
  lifetimePoints: number;
  redeemThreshold: number;
  canRedeem: boolean;
  progressPercent: number;
  weekendBonusActive: boolean;
}

/** Result of a cash redemption (mirrors backend RedeemResponse). */
export interface RedeemResponse {
  redeemedPoints: number;
  amountCredited: number;
  remainingPoints: number;
  transactionId: string;
  message: string;
}
