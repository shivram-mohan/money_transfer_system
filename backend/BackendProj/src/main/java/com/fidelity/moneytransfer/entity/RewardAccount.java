package com.fidelity.moneytransfer.entity;

import com.fidelity.moneytransfer.enums.Tier;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A user's standing in the rewards programme. One row per money-transfer
 * account. The point balance is the single source of truth: the tier is always
 * derived from {@link #points} via {@link Tier#fromPoints(long)}, and the cached
 * {@link #tier} column exists only so we can detect tier transitions (upgrade /
 * downgrade) and render the profile without recomputing.
 */
@Entity
@Table(name = "reward_accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The money-transfer account this reward balance belongs to. */
    @Column(name = "account_id", nullable = false, unique = true)
    private Long accountId;

    /** Owning user, kept for convenience when sending alert emails. */
    @Column(name = "user_id")
    private Long userId;

    /** Current redeemable reward-point balance; also drives the tier. */
    @Column(nullable = false)
    @Builder.Default
    private Long points = 0L;

    /** Cached tier, derived from {@link #points}; used for change detection. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Tier tier = Tier.BRONZE;

    /** Lifetime points ever earned (never decremented), for analytics/summary. */
    @Column(name = "lifetime_points", nullable = false)
    @Builder.Default
    private Long lifetimePoints = 0L;

    /** Timestamp of the last reward-earning transaction; drives inactivity. */
    @Column(name = "last_earned_date")
    private LocalDateTime lastEarnedDate;

    /** Set when the pre-downgrade warning email has been sent, cleared on activity. */
    @Column(name = "downgrade_warning_sent")
    @Builder.Default
    private Boolean downgradeWarningSent = false;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdDate = now;
        this.lastModifiedDate = now;
        if (this.tier == null) {
            this.tier = Tier.fromPoints(this.points == null ? 0L : this.points);
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.lastModifiedDate = LocalDateTime.now();
    }

    /**
     * Adds points, refreshes lifetime total and activity timestamp, and
     * re-derives the cached tier. Returns the tier that was in effect BEFORE
     * the change so the caller can detect an upgrade.
     */
    public Tier addPoints(long earned) {
        Tier previousTier = Tier.fromPoints(this.points);
        this.points += earned;
        this.lifetimePoints += earned;
        this.lastEarnedDate = LocalDateTime.now();
        this.downgradeWarningSent = false;
        this.tier = Tier.fromPoints(this.points);
        return previousTier;
    }
}
