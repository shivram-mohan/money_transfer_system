package com.fidelity.moneytransfer.service;

import com.fidelity.moneytransfer.entity.OtpToken;
import com.fidelity.moneytransfer.repository.OtpTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpTokenRepository otpTokenRepository;
    private final EmailService emailService;

    @Value("${app.otp.expiration:300}")
    private int otpExpirationSeconds;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public String generateAndSendOtp(String email, String purpose) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "No email address on record to send the OTP to");
        }

        String otp = String.format("%06d", secureRandom.nextInt(1000000));

        // Reuse the single OTP row for this email if it exists, otherwise create one.
        // This keeps the table at one row per user instead of growing on every request.
        OtpToken otpToken = otpTokenRepository.findTopByEmailOrderByIdDesc(email)
                .orElseGet(() -> OtpToken.builder().email(email).build());

        otpToken.setOtp(otp);
        otpToken.setPurpose(purpose);
        otpToken.setExpiresAt(LocalDateTime.now().plusSeconds(otpExpirationSeconds));

        otpTokenRepository.save(otpToken);

        // Dispatch the email asynchronously so the API responds immediately
        emailService.sendOtpEmail(email, otp, purpose);

        log.info("OTP generated for {} (email dispatch queued) for purpose: {}", email, purpose);
        return otp;
    }

    @Transactional
    public boolean verifyOtp(String email, String otp, String purpose) {
        OtpToken otpToken = otpTokenRepository
                .findTopByEmailOrderByIdDesc(email)
                .orElse(null);

        if (otpToken == null) {
            log.warn("No OTP found for email: {}", email);
            return false;
        }

        if (!otpToken.getPurpose().equals(purpose)) {
            log.warn("OTP purpose mismatch for email: {}", email);
            return false;
        }

        if (otpToken.isExpired()) {
            log.warn("OTP expired for email: {}", email);
            return false;
        }

        if (!otpToken.getOtp().equals(otp)) {
            log.warn("Invalid OTP for email: {}", email);
            return false;
        }

        // Consume the OTP so it cannot be reused
        otpTokenRepository.delete(otpToken);

        log.info("OTP verified successfully for email: {} purpose: {}", email, purpose);
        return true;
    }
}
