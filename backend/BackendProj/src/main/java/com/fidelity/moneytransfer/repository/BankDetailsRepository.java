package com.fidelity.moneytransfer.repository;

import com.fidelity.moneytransfer.entity.BankDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BankDetailsRepository extends JpaRepository<BankDetails, Long> {
    Optional<BankDetails> findByAccountNumber(Long accountNumber);
    boolean existsByAccountNumber(Long accountNumber);
}
