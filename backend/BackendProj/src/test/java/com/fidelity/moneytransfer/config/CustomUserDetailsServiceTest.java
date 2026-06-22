package com.fidelity.moneytransfer.config;

import com.fidelity.moneytransfer.entity.AppUser;
import com.fidelity.moneytransfer.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock private UserRepository userRepository;
    @InjectMocks private CustomUserDetailsService service;

    private AppUser user;

    @BeforeEach
    void setUp() {
        user = AppUser.builder()
                .id(1L).username("john").password("hash")
                .name("John").role("USER").status("ACTIVE")
                .build();
    }

    @Test
    void loadUserByUsername_ActiveUser_ReturnsUserDetailsWithRolePrefix() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("john");

        assertEquals("john", details.getUsername());
        assertEquals("hash", details.getPassword());
        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    void loadUserByUsername_AdminRole_MappedToRoleAdmin() {
        user.setRole("ADMIN");
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("john");

        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void loadUserByUsername_NotFound_Throws() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("ghost"));
    }

    @Test
    void loadUserByUsername_NotActive_Throws() {
        user.setStatus("INACTIVE");
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("john"));
    }
}
