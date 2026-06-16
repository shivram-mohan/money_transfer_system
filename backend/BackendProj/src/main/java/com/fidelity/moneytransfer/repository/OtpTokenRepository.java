package com.fidelity.moneytransfer.repository;

import com.fidelity.moneytransfer.entity.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {
    Optional<OtpToken> findTopByEmailAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(
            String email, String purpose);

    void deleteByEmail(String email);
}
