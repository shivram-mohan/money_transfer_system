package com.fidelity.moneytransfer;

import com.fidelity.moneytransfer.config.SecurityConfig;
import com.fidelity.moneytransfer.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Boots the full Spring context against H2. This exercises the wiring that
 * pure unit tests can't reach: the security filter chain, the authentication
 * manager bean, the async/CORS configuration, and the DataSeeder CommandLineRunner
 * (which seeds the admin, sample bank records and the CASHBACK account on startup).
 */
@SpringBootTest(classes = MoneyTransferApplication.class)
@ActiveProfiles("test")
class ApplicationContextTest {

    @Autowired private SecurityFilterChain securityFilterChain;
    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private SecurityConfig securityConfig;
    @Autowired private UserRepository userRepository;

    @Test
    void contextLoads_AndSecurityBeansArePresent() {
        assertNotNull(securityFilterChain);
        assertNotNull(authenticationManager);
        assertNotNull(securityConfig);
    }

    @Test
    void dataSeeder_SeededAdminUser() {
        // DataSeeder.run() executes on startup; the admin account must exist.
        assertTrue(userRepository.existsByUsername("admin"));
    }
}
