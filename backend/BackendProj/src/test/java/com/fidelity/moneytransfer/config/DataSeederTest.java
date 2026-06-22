package com.fidelity.moneytransfer.config;

import com.fidelity.moneytransfer.constants.RewardConstants;
import com.fidelity.moneytransfer.entity.AppUser;
import com.fidelity.moneytransfer.entity.Account;
import com.fidelity.moneytransfer.entity.BankDetails;
import com.fidelity.moneytransfer.repository.AccountRepository;
import com.fidelity.moneytransfer.repository.BankDetailsRepository;
import com.fidelity.moneytransfer.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataSeederTest {

    @Mock private UserRepository userRepository;
    @Mock private BankDetailsRepository bankDetailsRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JdbcTemplate jdbcTemplate;

    @InjectMocks private DataSeeder dataSeeder;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(dataSeeder, "adminUsername", "admin");
        ReflectionTestUtils.setField(dataSeeder, "adminPassword", "admin123");
        ReflectionTestUtils.setField(dataSeeder, "adminName", "System Administrator");
        ReflectionTestUtils.setField(dataSeeder, "adminEmail", "admin@moneytransfer.com");
    }

    @Test
    void run_freshDatabase_seedsEverything() {
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(passwordEncoder.encode("admin123")).thenReturn("hash");
        when(bankDetailsRepository.count()).thenReturn(0L);
        when(accountRepository.existsById(RewardConstants.CASHBACK_ACCOUNT_ID)).thenReturn(false);

        dataSeeder.run();

        verify(jdbcTemplate, times(2)).update(anyString());      // backfill reward cols
        verify(jdbcTemplate, atLeastOnce()).execute(anyString()); // otp migration
        verify(userRepository).save(any(AppUser.class));         // admin
        verify(bankDetailsRepository, times(5)).save(any(BankDetails.class)); // 5 records
        verify(accountRepository).save(any(Account.class));      // cashback
    }

    @Test
    void run_alreadySeeded_andMigrationsFail_skipsAndSwallows() {
        // Migrations throw -> caught defensively.
        doThrow(new RuntimeException("no table")).when(jdbcTemplate).execute(anyString());
        when(jdbcTemplate.update(anyString())).thenThrow(new RuntimeException("no column"));

        when(userRepository.existsByUsername("admin")).thenReturn(true);
        when(bankDetailsRepository.count()).thenReturn(7L);
        when(accountRepository.existsById(RewardConstants.CASHBACK_ACCOUNT_ID)).thenReturn(true);

        dataSeeder.run();

        verify(userRepository, never()).save(any());
        verify(bankDetailsRepository, never()).save(any());
        verify(accountRepository, never()).save(any());
    }
}
