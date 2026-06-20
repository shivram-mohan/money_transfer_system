package com.fidelity.moneytransfer.config;

import com.fidelity.moneytransfer.entity.AppUser;
import com.fidelity.moneytransfer.entity.BankDetails;
import com.fidelity.moneytransfer.repository.BankDetailsRepository;
import com.fidelity.moneytransfer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BankDetailsRepository bankDetailsRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:admin123}")
    private String adminPassword;

    @Value("${app.admin.name:System Administrator}")
    private String adminName;

    @Value("${app.admin.email:admin@moneytransfer.com}")
    private String adminEmail;

    @Override
    public void run(String... args) {
        migrateOtpTokens();
        seedAdmin();
        seedBankDetails();
    }

    /**
     * One-time cleanup for the OTP table redesign: the legacy schema had a
     * NOT NULL {@code verified} column and allowed multiple rows per email.
     * The table now keeps a single (unique) row per email, so we drop the old
     * column and clear stale rows. Wrapped defensively so a fresh database
     * (where the column never existed) is unaffected.
     */
    private void migrateOtpTokens() {
        try {
            jdbcTemplate.execute("DELETE FROM otp_tokens");
        } catch (Exception e) {
            log.debug("Skipping otp_tokens cleanup: {}", e.getMessage());
        }
        try {
            jdbcTemplate.execute("ALTER TABLE otp_tokens DROP COLUMN verified");
            log.info("Dropped legacy 'verified' column from otp_tokens");
        } catch (Exception e) {
            log.debug("No legacy 'verified' column to drop: {}", e.getMessage());
        }
    }

    private void seedAdmin() {
        if (userRepository.existsByUsername(adminUsername)) {
            log.info("Admin user already exists, skipping seed");
            return;
        }

        AppUser admin = AppUser.builder()
                .username(adminUsername)
                // Store the bcrypt hash of the admin password
                .password(passwordEncoder.encode(adminPassword))
                .name(adminName)
                .email(adminEmail)
                .role("ADMIN")
                .status("ACTIVE")
                .createdBy("system")
                .approvedBy("system")
                .build();

        userRepository.save(admin);
        log.info("Admin user seeded: {}", adminUsername);
    }

    private void seedBankDetails() {
        if (bankDetailsRepository.count() > 0) {
            log.info("Bank details already seeded, skipping");
            return;
        }

        // Sample bank records (simulating a pre-existing banking system)
        BankDetails[] sampleRecords = {
                BankDetails.builder()
                        .accountNumber(1001001001L)
                        .userName("John Doe")
                        .email("john.doe@example.com")
                        .balance(new BigDecimal("50000.00"))
                        .registered(false)
                        .build(),
                BankDetails.builder()
                        .accountNumber(1001001002L)
                        .userName("Jane Smith")
                        .email("jane.smith@example.com")
                        .balance(new BigDecimal("75000.00"))
                        .registered(false)
                        .build(),
                BankDetails.builder()
                        .accountNumber(1001001003L)
                        .userName("Bob Wilson")
                        .email("bob.wilson@example.com")
                        .balance(new BigDecimal("120000.00"))
                        .registered(false)
                        .build(),
                BankDetails.builder()
                        .accountNumber(1001001004L)
                        .userName("Alice Brown")
                        .email("alice.brown@example.com")
                        .balance(new BigDecimal("30000.00"))
                        .registered(false)
                        .build(),
                BankDetails.builder()
                        .accountNumber(1001001005L)
                        .userName("Charlie Davis")
                        .email("charlie.davis@example.com")
                        .balance(new BigDecimal("95000.00"))
                        .registered(false)
                        .build()
        };

        for (BankDetails record : sampleRecords) {
            bankDetailsRepository.save(record);
        }

        log.info("Seeded {} bank detail records", sampleRecords.length);
    }
}
