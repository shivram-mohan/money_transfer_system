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
}
