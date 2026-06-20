package com.fidelity.moneytransfer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Sends OTP emails off the request thread. The SMTP handshake with Gmail can
 * take several seconds, so blocking the HTTP request on it makes the API feel
 * frozen. The OTP is already persisted before this runs, so the user can verify
 * as soon as the mail arrives while the caller gets an immediate response.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.otp.expiration:300}")
    private int otpExpirationSeconds;

    @Async("otpMailExecutor")
    public void sendOtpEmail(String toEmail, String otp, String purpose) {
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
            } else if ("RESET".equals(purpose)) {
                message.setSubject("Money Transfer System - Password Reset OTP");
                message.setText(
                        "Your OTP to reset your password is: " + otp + "\n\n" +
                        "This OTP is valid for " + (otpExpirationSeconds / 60) + " minutes.\n\n" +
                        "If you did not request a password reset, please secure your account immediately."
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
        }
    }

    // ─── Rewards emails ──────────────────────────────────────────────────

    @Async("otpMailExecutor")
    public void sendMonthlySummaryEmail(String toEmail, String name, String month,
                                        long rewardedTransfers, long pointsEarned,
                                        String cashbackEarned, String tier, long totalPoints) {
        String body = "Hi " + name + ",\n\n" +
                "Here's your Rewards summary for " + month + ":\n\n" +
                "  • Rewarded transfers : " + rewardedTransfers + "\n" +
                "  • Points earned      : " + pointsEarned + "\n" +
                "  • Cashback earned    : ₹" + cashbackEarned + "\n\n" +
                "Current tier   : " + tier + "\n" +
                "Total points   : " + totalPoints + "\n\n" +
                "Keep transferring to climb the tiers and unlock bigger multipliers!\n\n" +
                "— Money Transfer Rewards";
        send(toEmail, "Your " + month + " Rewards Summary", body);
    }

    @Async("otpMailExecutor")
    public void sendDowngradeWarningEmail(String toEmail, String name, String tier,
                                          long daysUntilDowngrade) {
        String body = "Hi " + name + ",\n\n" +
                "Your " + tier + " tier is at risk! You haven't made a transfer recently.\n\n" +
                "If you stay inactive, your tier will be downgraded in about "
                + daysUntilDowngrade + " day(s).\n\n" +
                "Make a transfer now to keep your tier and multiplier.\n\n" +
                "— Money Transfer Rewards";
        send(toEmail, "Don't lose your " + tier + " tier", body);
    }

    @Async("otpMailExecutor")
    public void sendDowngradeEmail(String toEmail, String name, String oldTier, String newTier) {
        String body = "Hi " + name + ",\n\n" +
                "Due to 30 days of inactivity, your tier has been downgraded from "
                + oldTier + " to " + newTier + ".\n\n" +
                "Start transferring again to earn points and climb back up!\n\n" +
                "— Money Transfer Rewards";
        send(toEmail, "Your tier was downgraded to " + newTier, body);
    }

    private void send(String toEmail, String subject, String body) {
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Skipping rewards email '{}' — no recipient address", subject);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Money Transfer System - " + subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Rewards email '{}' sent to: {}", subject, toEmail);
        } catch (Exception e) {
            log.error("Failed to send rewards email to {}: {}", toEmail, e.getMessage());
        }
    }
}
