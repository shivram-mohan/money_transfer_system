//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.fidelity.moneytransfer.repository;

import com.fidelity.moneytransfer.entity.TransactionLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionLogRepository extends JpaRepository<TransactionLog, String> {
    Optional<TransactionLog> findByIdempotencyKey(String idempotencyKey);
    @Query("SELECT t FROM TransactionLog t WHERE " +
            "(t.fromAccountId = :accountId OR t.toAccountId = :accountId) " +
            "AND t.createdOn BETWEEN :startDate AND :endDate " +
            "ORDER BY t.createdOn DESC")
    List<TransactionLog> findByAccountIdAndDateRange(
            @Param("accountId") Long accountId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    List<TransactionLog> findByFromAccountIdOrToAccountIdOrderByCreatedOnDesc(Long fromAccountId, Long toAccountId);
}
