package com.fidelity.moneytransfer.config;

import com.fidelity.moneytransfer.entity.Account;
import com.fidelity.moneytransfer.repository.AccountRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Optional;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Admin only endpoints
                        .requestMatchers(
                                org.springframework.http.HttpMethod.POST,
                                "/api/v1/accounts"
                        ).hasRole("ADMIN")
                        .requestMatchers(
                                org.springframework.http.HttpMethod.PUT,
                                "/api/v1/accounts/*/activate"
                        ).hasRole("ADMIN")
                        .requestMatchers(
                                org.springframework.http.HttpMethod.PUT,
                                "/api/v1/accounts/*/deactivate"
                        ).hasRole("ADMIN")
                        .requestMatchers(
                                org.springframework.http.HttpMethod.GET,
                                "/api/v1/accounts"
                        ).hasRole("ADMIN")
                        // All API endpoints require authentication
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().permitAll()
                )
                .httpBasic(withDefaults());

        return http.build();
    }

    @Bean
    public InMemoryUserDetailsManager inMemoryUserDetailsManager(PasswordEncoder passwordEncoder) {
        // Admin user (not stored in accounts table)
        UserDetails admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .roles("ADMIN", "USER")
                .build();

        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    public UserDetailsService userDetailsService(AccountRepository accountRepository,
                                                  InMemoryUserDetailsManager inMemoryManager) {
        return username -> {
            // First check in-memory users (admin)
            try {
                UserDetails inMemoryUser = inMemoryManager.loadUserByUsername(username);
                if (inMemoryUser != null) {
                    return inMemoryUser;
                }
            } catch (UsernameNotFoundException ignored) {
                // Not an in-memory user, check DB
            }

            // Then check database accounts
            Optional<Account> account = accountRepository.findByUsername(username);
            if (account.isPresent()) {
                Account acc = account.get();
                return User.builder()
                        .username(acc.getUsername())
                        .password(acc.getPassword())
                        .roles("USER")
                        .build();
            }

            throw new UsernameNotFoundException("User not found: " + username);
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
