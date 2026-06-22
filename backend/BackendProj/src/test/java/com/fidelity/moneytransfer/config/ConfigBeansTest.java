package com.fidelity.moneytransfer.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.CorsFilter;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Covers the simple @Bean factory methods on the configuration classes.
 */
class ConfigBeansTest {

    @Test
    void userDetailsServiceImpl_exposesBeans() {
        CustomUserDetailsService custom = mock(CustomUserDetailsService.class);
        UserDetailsServiceImpl config = new UserDetailsServiceImpl(custom);

        UserDetailsService uds = config.userDetailsService();
        assertSame(custom, uds);

        PasswordEncoder encoder = config.passwordEncoder();
        assertTrue(encoder instanceof BCryptPasswordEncoder);
        assertTrue(encoder.matches("secret", encoder.encode("secret")));
    }

    @Test
    void corsConfig_buildsFilter() {
        CorsFilter filter = new CorsConfig().corsFilter();
        assertNotNull(filter);
    }

    @Test
    void asyncConfig_buildsExecutor() {
        Executor executor = new AsyncConfig().otpMailExecutor();
        assertNotNull(executor);
    }
}
