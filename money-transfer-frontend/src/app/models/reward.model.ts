// src/app/models/reward.model.ts

export type Tier = 'BRONZE' | 'SILVER' | 'GOLD' | 'PLATINUM';

/** Outcome of a single transfer's rewards (mirrors backend RewardResult). */
export interface RewardResult {
  rewarded: boolean;
  pointsEarned: number;
  cashbackAmount: number;
  totalPoints: number;
  tier: Tier;
  nextTier: Tier | null;
  pointsToNextTier: number;
  tierUpgraded: boolean;
  closeToNextTier: boolean;
  message: string;
}

/** A user's current rewards standing (mirrors backend RewardProfileResponse). */
export interface RewardProfile {
  holderName: string;
  totalPoints: number;
  tier: Tier;
  multiplier: number;
  nextTier: Tier | null;
  pointsToNextTier: number;
  progressPercent: number;
  lastTransactionDate: string | null;
  totalCashbackEarned: number;
}

export interface MonthlySummary {
  month: string;
  rewardedTransfers: number;
  pointsEarned: number;
  cashbackEarned: number;
  tier: Tier;
  totalPoints: number;
}

/** Visual identity for each tier — the rewards module is intentionally
 *  colorful (the rest of the app stays black & white). */
export interface TierMeta {
  label: string;
  icon: string;          // Material icon name
  color: string;         // accent color
  gradient: string;      // badge background
  glow: string;          // box-shadow glow
}

export const TIER_META: Record<Tier, TierMeta> = {
  BRONZE: {
    label: 'Bronze',
    icon: 'military_tech',
    color: '#cd7f32',
    gradient: 'linear-gradient(135deg, #b06a2c 0%, #e8a565 100%)',
    glow: '0 0 24px rgba(205, 127, 50, 0.55)'
  },
  SILVER: {
    label: 'Silver',
    icon: 'military_tech',
    color: '#bfc7d1',
    gradient: 'linear-gradient(135deg, #8e99a8 0%, #e8eef5 100%)',
    glow: '0 0 24px rgba(191, 199, 209, 0.6)'
  },
  GOLD: {
    label: 'Gold',
    icon: 'workspace_premium',
    color: '#ffd700',
    gradient: 'linear-gradient(135deg, #e6b800 0%, #fff1a8 100%)',
    glow: '0 0 28px rgba(255, 215, 0, 0.6)'
  },
  PLATINUM: {
    label: 'Platinum',
    icon: 'diamond',
    color: '#7fdfff',
    gradient: 'linear-gradient(135deg, #00b4d8 0%, #b8f2ff 60%, #c8a2ff 100%)',
    glow: '0 0 32px rgba(127, 223, 255, 0.7)'
  }
};
