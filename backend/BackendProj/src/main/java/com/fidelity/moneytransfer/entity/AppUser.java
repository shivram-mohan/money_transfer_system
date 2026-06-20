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
    // Lifetime reward points; the user's tier is derived from this value.
    @Column(name = "reward_points")
    private Long rewardPoints;

    @Column(name = "tier", length = 20)
    private String tier;

    // Timestamp of the user's most recent (sent) transfer; drives the
    // 30-day inactivity downgrade.
    @Column(name = "last_transaction_date")
    private LocalDateTime lastTransactionDate;

    // Guards against re-sending the pre-downgrade warning every day.
    @Column(name = "downgrade_warning_sent")
    private Boolean downgradeWarningSent;

    @PrePersist
    public void prePersist() {
        this.createdDate = LocalDateTime.now();
        this.lastModifiedDate = LocalDateTime.now();
        if (this.rewardPoints == null) {
            this.rewardPoints = 0L;
        }
        if (this.tier == null) {
            this.tier = com.fidelity.moneytransfer.enums.Tier.BRONZE.name();
        }
        if (this.downgradeWarningSent == null) {
            this.downgradeWarningSent = false;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.lastModifiedDate = LocalDateTime.now();
    }
}