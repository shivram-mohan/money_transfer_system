package com.fidelity.moneytransfer.repository;

import com.fidelity.moneytransfer.entity.RewardAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RewardAccountRepository extends JpaRepository<RewardAccount, Long> {

    Optional<RewardAccount> findByAccountId(Long accountId);

    Optional<RewardAccount> findByUserId(Long userId);

    /**
     * Reward accounts that have earned points at least once and whose last
     * earning activity was on or before the given cut-off. Used by the
     * inactivity scheduler for downgrade warnings and downgrades.
     */
    List<RewardAccount> findByLastEarnedDateIsNotNullAndLastEarnedDateBefore(LocalDateTime cutoff);
}
