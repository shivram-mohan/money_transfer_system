package com.fidelity.moneytransfer.entity;

import com.fidelity.moneytransfer.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transaction_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionLog {

    @Id
    private String id;

    @Column(name = "from_account", nullable = false)
    private Long fromAccountId;

    @Column(name = "to_account", nullable = false)
    private Long toAccountId;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "idempotency_key", unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "created_on")
    private LocalDateTime createdOn;
    @Transient
    private String fromAccountHolderName;

    @Transient
    private String toAccountHolderName;


    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.createdOn == null) {
            this.createdOn = LocalDateTime.now();
        }
    }
}