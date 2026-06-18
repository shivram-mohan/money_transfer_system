package com.fidelity.moneytransfer.service;

import com.fidelity.moneytransfer.dto.RewardResponse;
import com.fidelity.moneytransfer.enums.Tier;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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

    // ─── REWARDS EMAILS (HTML) ───────────────────────────────────────

    /**
     * Sent after every reward-earning transfer: points gained, current balance,
     * tier, and how far to the next tier. Doubles as the tier-upgrade alert when
     * {@code upgraded} is true.
     */
    @Async("rewardMailExecutor")
    public void sendRewardEarnedEmail(String toEmail, String name, long pointsEarned,
                                      RewardResponse rewards, boolean upgraded) {
        String headline = upgraded
                ? "🎉 Tier Upgrade Unlocked!"
                : "🎁 You earned reward points!";
        String intro = upgraded
                ? "Congratulations" + namePart(name) + " — your latest transfer pushed you up to <b>"
                  + rewards.getTierName() + "</b> tier!"
                : "Nice one" + namePart(name) + "! You just earned <b>" + pointsEarned
                  + "</b> reward point" + (pointsEarned == 1 ? "" : "s") + " on your transfer.";

        String body = card(rewards,
                "<p style='font-size:16px;color:#374151;margin:0 0 16px;'>" + intro + "</p>"
                + statRow("Points earned", "+" + pointsEarned)
                + progressSection(rewards));

        sendHtml(toEmail, headline + " — Rewards", wrap(headline, rewards, body));
    }

    /**
     * Sent before an inactivity downgrade so the user has time to act.
     */
    @Async("rewardMailExecutor")
    public void sendDowngradeWarningEmail(String toEmail, String name, Tier currentTier,
                                          long daysRemaining, RewardResponse rewards) {
        String headline = "⏳ Keep your " + currentTier.getDisplayName() + " tier";
        String body = card(rewards,
                "<p style='font-size:16px;color:#374151;margin:0 0 16px;'>Hi" + namePart(name)
                + ", you haven't made a qualifying transfer in a while. Make one within <b>"
                + daysRemaining + " day" + (daysRemaining == 1 ? "" : "s") + "</b> to keep your <b>"
                + currentTier.getDisplayName() + "</b> tier and its "
                + rewards.getMultiplier() + "x points multiplier.</p>"
                + statRow("Current balance", rewards.getPoints() + " pts"));

        sendHtml(toEmail, headline, wrap(headline, rewards, body));
    }

    /**
     * Sent when a user's tier has been downgraded due to inactivity.
     */
    @Async("rewardMailExecutor")
    public void sendTierDowngradeEmail(String toEmail, String name, Tier oldTier,
                                       Tier newTier, RewardResponse rewards) {
        String headline = "Your tier changed to " + newTier.getDisplayName();
        String body = card(rewards,
                "<p style='font-size:16px;color:#374151;margin:0 0 16px;'>Hi" + namePart(name)
                + ", after a period of inactivity your tier has moved from <b>" + oldTier.getDisplayName()
                + "</b> to <b>" + newTier.getDisplayName() + "</b>. Make a transfer to start climbing back up!</p>"
                + progressSection(rewards));

        sendHtml(toEmail, "🔔 " + headline, wrap(headline, rewards, body));
    }

    /**
     * Monthly snapshot of the user's rewards standing.
     */
    @Async("rewardMailExecutor")
    public void sendMonthlySummaryEmail(String toEmail, String name, RewardResponse rewards) {
        String headline = "📊 Your Monthly Rewards Summary";
        String body = card(rewards,
                "<p style='font-size:16px;color:#374151;margin:0 0 16px;'>Hi" + namePart(name)
                + ", here's where your rewards stand this month.</p>"
                + statRow("Current balance", rewards.getPoints() + " pts")
                + statRow("Lifetime points", rewards.getLifetimePoints() + " pts")
                + statRow("Current tier", rewards.getTierName() + " (" + rewards.getMultiplier() + "x)")
                + progressSection(rewards));

        sendHtml(toEmail, headline, wrap(headline, rewards, body));
    }

    // ─── HTML HELPERS ────────────────────────────────────────────────

    private void sendHtml(String toEmail, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Rewards email '{}' sent to {}", subject, toEmail);
        } catch (Exception e) {
            log.error("Failed to send rewards email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String namePart(String name) {
        return (name == null || name.isBlank()) ? "" : " " + name;
    }

    private String wrap(String headline, RewardResponse rewards, String inner) {
        return "<div style=\"margin:0;padding:24px;background:#0f172a;font-family:'Segoe UI',Arial,sans-serif;\">"
                + "<div style=\"max-width:480px;margin:0 auto;background:#ffffff;border-radius:16px;overflow:hidden;"
                + "box-shadow:0 10px 30px rgba(0,0,0,0.25);\">"
                + "<div style=\"background:linear-gradient(135deg," + rewards.getTierColor() + "33,#1e293b);"
                + "padding:28px 24px;text-align:center;\">"
                + "<div style=\"font-size:44px;line-height:1;\">" + rewards.getTierIcon() + "</div>"
                + "<h1 style=\"color:#ffffff;font-size:20px;margin:12px 0 0;\">" + headline + "</h1>"
                + "</div>"
                + "<div style=\"padding:24px;\">" + inner + "</div>"
                + "<div style=\"padding:16px 24px;background:#f1f5f9;text-align:center;color:#94a3b8;font-size:12px;\">"
                + "Money Transfer System • Rewards Programme</div>"
                + "</div></div>";
    }

    private String card(RewardResponse rewards, String inner) {
        return "<div style=\"display:inline-block;width:100%;text-align:center;padding:14px 0 20px;\">"
                + "<span style=\"display:inline-block;padding:6px 16px;border-radius:999px;font-size:13px;"
                + "font-weight:600;color:#1e293b;background:" + rewards.getTierColor() + ";\">"
                + rewards.getTierIcon() + " " + rewards.getTierName() + " Tier • "
                + rewards.getMultiplier() + "x</span></div>" + inner;
    }

    private String statRow(String label, String value) {
        return "<div style=\"display:flex;justify-content:space-between;padding:10px 0;"
                + "border-bottom:1px solid #f1f5f9;font-size:15px;\">"
                + "<span style=\"color:#64748b;\">" + label + "</span>"
                + "<span style=\"color:#0f172a;font-weight:600;\">" + value + "</span></div>";
    }

    private String progressSection(RewardResponse rewards) {
        if (rewards.getNextTier() == null) {
            return "<p style=\"margin:18px 0 0;text-align:center;color:#16a34a;font-weight:600;\">"
                    + "You're at the top tier — enjoy the maximum multiplier! 🚀</p>";
        }
        int pct = rewards.getProgressPercent();
        return "<p style=\"margin:18px 0 8px;color:#374151;font-size:14px;\">"
                + "<b>" + rewards.getPointsToNextTier() + "</b> points to <b>"
                + rewards.getNextTier() + "</b></p>"
                + "<div style=\"background:#e2e8f0;border-radius:999px;height:12px;overflow:hidden;\">"
                + "<div style=\"width:" + pct + "%;height:12px;border-radius:999px;"
                + "background:linear-gradient(90deg,#6366f1,#8b5cf6);\"></div></div>";
    }
}
