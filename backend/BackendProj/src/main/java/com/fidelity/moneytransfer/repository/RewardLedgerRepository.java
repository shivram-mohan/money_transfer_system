package com.fidelity.moneytransfer.repository;

import com.fidelity.moneytransfer.entity.RewardLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface RewardLedgerRepository extends JpaRepository<RewardLedger, Long> {

    /** True if this directed pair has already been rewarded on the given day. */
    boolean existsByFromAccountIdAndToAccountIdAndRewardDate(
            Long fromAccountId, Long toAccountId, LocalDate rewardDate);
}
