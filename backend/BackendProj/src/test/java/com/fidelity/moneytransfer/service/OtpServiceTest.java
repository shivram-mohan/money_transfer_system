package com.fidelity.moneytransfer.service;

import com.fidelity.moneytransfer.entity.OtpToken;
import com.fidelity.moneytransfer.repository.OtpTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock private OtpTokenRepository otpTokenRepository;
    @Mock private EmailService emailService;

    @InjectMocks private OtpService otpService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(otpService, "otpExpirationSeconds", 300);
    }

    @Test
    void generateAndSendOtp_nullEmail_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> otpService.generateAndSendOtp(null, "SIGNUP"));
    }

    @Test
    void generateAndSendOtp_blankEmail_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> otpService.generateAndSendOtp("  ", "SIGNUP"));
    }

    @Test
    void generateAndSendOtp_newToken_savesAndSends() {
        when(otpTokenRepository.findTopByEmailOrderByIdDesc("a@b.com"))
                .thenReturn(Optional.empty());
        when(otpTokenRepository.save(any(OtpToken.class))).thenAnswer(inv -> inv.getArgument(0));

        String otp = otpService.generateAndSendOtp("a@b.com", "SIGNUP");

        assertNotNull(otp);
        assertEquals(6, otp.length());
        verify(otpTokenRepository).save(any(OtpToken.class));
        verify(emailService).sendOtpEmail(eq("a@b.com"), eq(otp), eq("SIGNUP"));
    }

    @Test
    void generateAndSendOtp_existingToken_reused() {
        OtpToken existing = OtpToken.builder().id(1L).email("a@b.com").otp("000000")
                .purpose("LOGIN").expiresAt(LocalDateTime.now()).build();
        when(otpTokenRepository.findTopByEmailOrderByIdDesc("a@b.com"))
                .thenReturn(Optional.of(existing));
        when(otpTokenRepository.save(any(OtpToken.class))).thenAnswer(inv -> inv.getArgument(0));

        otpService.generateAndSendOtp("a@b.com", "LOGIN");

        assertEquals("LOGIN", existing.getPurpose());
        verify(otpTokenRepository).save(existing);
    }

    @Test
    void verifyOtp_noToken_false() {
        when(otpTokenRepository.findTopByEmailOrderByIdDesc("a@b.com")).thenReturn(Optional.empty());
        assertFalse(otpService.verifyOtp("a@b.com", "123456", "SIGNUP"));
    }

    @Test
    void verifyOtp_purposeMismatch_false() {
        OtpToken token = OtpToken.builder().email("a@b.com").otp("123456").purpose("LOGIN")
                .expiresAt(LocalDateTime.now().plusMinutes(5)).build();
        when(otpTokenRepository.findTopByEmailOrderByIdDesc("a@b.com")).thenReturn(Optional.of(token));
        assertFalse(otpService.verifyOtp("a@b.com", "123456", "SIGNUP"));
    }

    @Test
    void verifyOtp_expired_false() {
        OtpToken token = OtpToken.builder().email("a@b.com").otp("123456").purpose("SIGNUP")
                .expiresAt(LocalDateTime.now().minusMinutes(1)).build();
        when(otpTokenRepository.findTopByEmailOrderByIdDesc("a@b.com")).thenReturn(Optional.of(token));
        assertFalse(otpService.verifyOtp("a@b.com", "123456", "SIGNUP"));
    }

    @Test
    void verifyOtp_wrongCode_false() {
        OtpToken token = OtpToken.builder().email("a@b.com").otp("123456").purpose("SIGNUP")
                .expiresAt(LocalDateTime.now().plusMinutes(5)).build();
        when(otpTokenRepository.findTopByEmailOrderByIdDesc("a@b.com")).thenReturn(Optional.of(token));
        assertFalse(otpService.verifyOtp("a@b.com", "999999", "SIGNUP"));
        verify(otpTokenRepository, never()).delete(any());
    }

    @Test
    void verifyOtp_valid_consumesAndTrue() {
        OtpToken token = OtpToken.builder().email("a@b.com").otp("123456").purpose("SIGNUP")
                .expiresAt(LocalDateTime.now().plusMinutes(5)).build();
        when(otpTokenRepository.findTopByEmailOrderByIdDesc("a@b.com")).thenReturn(Optional.of(token));
        assertTrue(otpService.verifyOtp("a@b.com", "123456", "SIGNUP"));
        verify(otpTokenRepository).delete(token);
    }
}
