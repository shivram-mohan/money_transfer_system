package com.fidelity.moneytransfer.repository;

import com.fidelity.moneytransfer.entity.RewardLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RewardLedgerRepository extends JpaRepository<RewardLedger, Long> {

    /** True if this directed pair has already been rewarded on the given day. */
    boolean existsByFromAccountIdAndToAccountIdAndRewardDate(
            Long fromAccountId, Long toAccountId, LocalDate rewardDate);

    List<RewardLedger> findByUserIdAndCreatedOnBetween(
            Long userId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT COALESCE(SUM(r.pointsEarned), 0) FROM RewardLedger r " +
            "WHERE r.userId = :userId AND r.createdOn BETWEEN :start AND :end")
    long sumPointsEarned(@Param("userId") Long userId,
                         @Param("start") LocalDateTime start,
                         @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(r.cashbackAmount), 0) FROM RewardLedger r " +
            "WHERE r.userId = :userId AND r.createdOn BETWEEN :start AND :end")
    BigDecimal sumCashback(@Param("userId") Long userId,
                           @Param("start") LocalDateTime start,
                           @Param("end") LocalDateTime end);

    long countByUserIdAndCreatedOnBetween(
            Long userId, LocalDateTime start, LocalDateTime end);
}
