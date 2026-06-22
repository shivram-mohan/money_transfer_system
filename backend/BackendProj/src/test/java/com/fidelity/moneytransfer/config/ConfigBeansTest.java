package com.fidelity.moneytransfer.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.CorsFilter;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Covers the lightweight @Configuration bean factory methods that don't warrant
 * a full Spring context.
 */
class ConfigBeansTest {

    @Test
    void corsConfig_BuildsFilter() {
        CorsFilter filter = new CorsConfig().corsFilter();
        assertNotNull(filter);
    }

    @Test
    void asyncConfig_BuildsExecutor() {
        Executor executor = new AsyncConfig().otpMailExecutor();
        assertNotNull(executor);
    }

    @Test
    void userDetailsServiceImpl_ExposesBeans() {
        CustomUserDetailsService custom = mock(CustomUserDetailsService.class);
        UserDetailsServiceImpl config = new UserDetailsServiceImpl(custom);

        UserDetailsService uds = config.userDetailsService();
        PasswordEncoder encoder = config.passwordEncoder();

        assertSame(custom, uds);
        assertNotNull(encoder);
        // BCrypt produces a verifiable hash
        String hash = encoder.encode("secret");
        assertTrue(encoder.matches("secret", hash));
    }

    @Test
    void securityConfig_AuthenticationProvider_NotNull() {
        SecurityConfig config = new SecurityConfig(
                mock(JwtAuthFilter.class),
                mock(JwtAuthEntryPoint.class),
                mock(UserDetailsService.class),
                mock(PasswordEncoder.class));

        AuthenticationProvider provider = config.authenticationProvider();
        assertNotNull(provider);
    }
}
