package com.fidelity.moneytransfer.config;

import com.fidelity.moneytransfer.entity.AppUser;
import com.fidelity.moneytransfer.entity.BankDetails;
import com.fidelity.moneytransfer.repository.BankDetailsRepository;
import com.fidelity.moneytransfer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
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
        seedAdmin();
        seedBankDetails();
    }

    private void seedAdmin() {
        if (userRepository.existsByUsername(adminUsername)) {
            log.info("Admin user already exists, skipping seed");
            return;
        }

        AppUser admin = AppUser.builder()
                .username(adminUsername)
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
