package com.fidelity.moneytransfer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One row per rewarded transfer. It is the audit trail for points/cashback and
 * the source for the monthly summary, and its (fromAccountId, toAccountId,
 * rewardDate) is used to enforce "only the first transfer per day between the
 * same directed pair earns rewards".
 */
@Entity
@Table(
        name = "reward_ledger",
        indexes = {
                @Index(name = "idx_reward_pair_date",
                        columnList = "from_account, to_account, reward_date"),
                @Index(name = "idx_reward_user_date",
                        columnList = "user_id, created_on")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "from_account", nullable = false)
    private Long fromAccountId;

    @Column(name = "to_account", nullable = false)
    private Long toAccountId;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "points_earned", nullable = false)
    private Long pointsEarned;

    @Column(name = "cashback_amount", precision = 18, scale = 2)
    private BigDecimal cashbackAmount;

    @Column(name = "reward_date", nullable = false)
    private LocalDate rewardDate;

    @Column(name = "created_on", nullable = false)
    private LocalDateTime createdOn;

    @PrePersist
    public void prePersist() {
        if (this.createdOn == null) {
            this.createdOn = LocalDateTime.now();
        }
        if (this.rewardDate == null) {
            this.rewardDate = this.createdOn.toLocalDate();
        }
    }
}
