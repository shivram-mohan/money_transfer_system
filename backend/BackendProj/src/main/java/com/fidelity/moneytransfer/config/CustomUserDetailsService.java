package com.fidelity.moneytransfer.config;

import com.fidelity.moneytransfer.entity.AppUser;
import com.fidelity.moneytransfer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        log.debug("Loading user: {}", username);

        // Load ALL users (including admin) from database
        AppUser appUser = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found: {}", username);
                    return new UsernameNotFoundException(
                            "User not found: " + username
                    );
                });

        // Check if user is active
        if (!appUser.getStatus().equals("ACTIVE")) {
            log.error("User not active: {}", username);
            throw new UsernameNotFoundException(
                    "Account is not active"
            );
        }

        log.debug("Loaded user: {} role: {}",
                username, appUser.getRole());

        return User.builder()
                .username(appUser.getUsername())
                .password(appUser.getPassword())
                .authorities(Collections.singletonList(
                        new SimpleGrantedAuthority(
                                "ROLE_" + appUser.getRole()
                        )
                ))
                .build();
    }
}