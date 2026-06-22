package com.fidelity.moneytransfer.config;

import com.fidelity.moneytransfer.constants.RewardConstants;
import com.fidelity.moneytransfer.entity.Account;
import com.fidelity.moneytransfer.entity.AppUser;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataSeederTest {

    @Mock private UserRepository userRepository;
    @Mock private BankDetailsRepository bankDetailsRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JdbcTemplate jdbcTemplate;

    @InjectMocks private DataSeeder seeder;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(seeder, "adminUsername", "admin");
        ReflectionTestUtils.setField(seeder, "adminPassword", "admin123");
        ReflectionTestUtils.setField(seeder, "adminName", "System Administrator");
        ReflectionTestUtils.setField(seeder, "adminEmail", "admin@moneytransfer.com");
    }

    @Test
    void run_FreshDatabase_SeedsEverything() {
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(passwordEncoder.encode("admin123")).thenReturn("bcrypt");
        when(bankDetailsRepository.count()).thenReturn(0L);
        when(accountRepository.existsById(RewardConstants.CASHBACK_ACCOUNT_ID))
                .thenReturn(false);

        seeder.run();

        verify(userRepository).save(any(AppUser.class));
        verify(bankDetailsRepository, times(5)).save(any());
        verify(accountRepository).save(any(Account.class));
        verify(jdbcTemplate, atLeastOnce()).execute(anyString());
        verify(jdbcTemplate, atLeastOnce()).update(anyString());
    }

    @Test
    void run_AlreadySeeded_SkipsSeeds_AndSwallowsJdbcErrors() {
        // jdbc migration/backfill throw -> exercise the defensive catch blocks
        when(jdbcTemplate.update(anyString()))
                .thenThrow(new RuntimeException("no such column"));
        doThrow(new RuntimeException("no such table"))
                .when(jdbcTemplate).execute(anyString());

        when(userRepository.existsByUsername("admin")).thenReturn(true);
        when(bankDetailsRepository.count()).thenReturn(5L);
        when(accountRepository.existsById(RewardConstants.CASHBACK_ACCOUNT_ID))
                .thenReturn(true);

        seeder.run();

        verify(userRepository, never()).save(any(AppUser.class));
        verify(bankDetailsRepository, never()).save(any());
        verify(accountRepository, never()).save(any(Account.class));
    }
}
