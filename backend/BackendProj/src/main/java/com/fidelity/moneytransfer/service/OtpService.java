package com.fidelity.moneytransfer.service;

import com.fidelity.moneytransfer.entity.OtpToken;
import com.fidelity.moneytransfer.repository.OtpTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpTokenRepository otpTokenRepository;
    private final JavaMailSender mailSender;

    @Value("${app.otp.expiration:300}")
    private int otpExpirationSeconds;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public String generateAndSendOtp(String email, String purpose) {
        String otp = String.format("%06d", secureRandom.nextInt(1000000));

        OtpToken otpToken = OtpToken.builder()
                .email(email)
                .otp(otp)
                .purpose(purpose)
                .expiresAt(LocalDateTime.now().plusSeconds(otpExpirationSeconds))
                .verified(false)
                .build();

        otpTokenRepository.save(otpToken);

        sendOtpEmail(email, otp, purpose);

        log.info("OTP generated and sent to {} for purpose: {}", email, purpose);
        return otp;
    }

    @Transactional
    public boolean verifyOtp(String email, String otp, String purpose) {
        OtpToken otpToken = otpTokenRepository
                .findTopByEmailAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(email, purpose)
                .orElse(null);

        if (otpToken == null) {
            log.warn("No OTP found for email: {} purpose: {}", email, purpose);
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

        otpToken.setVerified(true);
        otpTokenRepository.save(otpToken);

        log.info("OTP verified successfully for email: {} purpose: {}", email, purpose);
        return true;
    }

    private void sendOtpEmail(String toEmail, String otp, String purpose) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);

            if ("SIGNUP".equals(purpose)) {
                message.setSubject("Money Transfer System - Account Verification OTP");
                message.setText(
                        "Your OTP for account verification is: " + otp + "\n\n" +
                        "This OTP is valid for " + (otpExpirationSeconds / 60) + " minutes.\n\n" +
                        "If you did not request this, please ignore this email."
                );
            } else {
                message.setSubject("Money Transfer System - Login OTP");
                message.setText(
                        "Your OTP for login is: " + otp + "\n\n" +
                        "This OTP is valid for " + (otpExpirationSeconds / 60) + " minutes.\n\n" +
                        "If you did not request this, please secure your account immediately."
                );
            }

            mailSender.send(message);
            log.info("OTP email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send OTP email to: {} - {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send OTP email. Please try again.");
        }
    }
}
