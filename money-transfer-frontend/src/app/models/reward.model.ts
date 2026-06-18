// src/app/models/reward.model.ts

/** A user's current rewards standing (mirrors backend RewardResponse). */
export interface RewardResponse {
  accountId: number;
  points: number;
  lifetimePoints: number;
  tier: string;        // enum name e.g. "GOLD"
  tierName: string;    // display name e.g. "Gold"
  tierIcon: string;    // emoji
  tierColor: string;   // hex colour
  multiplier: number;
  nextTier: string | null;
  pointsToNextTier: number;
  progressPercent: number;
}

/** Outcome of awarding points for a single transfer (mirrors RewardEarnResult). */
export interface RewardEarnResult {
  earned: boolean;
  pointsEarned: number;
  basePoints: number;
  tierUpgraded: boolean;
  nearNextTier: boolean;
  rewards: RewardResponse;
}
