package com.fidelity.moneytransfer.repository;

import com.fidelity.moneytransfer.entity.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    // Most recent (and, going forward, only) OTP row for an email
    Optional<OtpToken> findTopByEmailOrderByIdDesc(String email);

    void deleteByEmail(String email);
}
