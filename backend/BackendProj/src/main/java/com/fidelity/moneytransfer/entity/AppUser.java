package com.fidelity.moneytransfer.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column
    private String email;

    @Column(nullable = false)
    private String role; // USER, ADMIN

    @Column(nullable = false)
    private String status; // ACTIVE, INACTIVE, PENDING, LOCKED

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "approved_date")
    private LocalDateTime approvedDate;

    // ─── Rewards ────────────────────────────────────────────────────
    // Spendable reward-point balance (decreases when points are redeemed).
    @Column(name = "reward_points")
    private Long rewardPoints;

    // Total points ever earned; only ever increases (shown as "lifetime").
    @Column(name = "lifetime_reward_points")
    private Long lifetimeRewardPoints;

    @PrePersist
    public void prePersist() {
        this.createdDate = LocalDateTime.now();
        this.lastModifiedDate = LocalDateTime.now();
        if (this.rewardPoints == null) {
            this.rewardPoints = 0L;
        }
        if (this.lifetimeRewardPoints == null) {
            this.lifetimeRewardPoints = 0L;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.lastModifiedDate = LocalDateTime.now();
    }
}