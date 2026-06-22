package com.fidelity.moneytransfer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock private JavaMailSender mailSender;

    @InjectMocks private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@test.com");
        ReflectionTestUtils.setField(emailService, "otpExpirationSeconds", 300);
    }

    @Test
    void sendOtpEmail_signup_subjectAndBody() {
        emailService.sendOtpEmail("user@test.com", "123456", "SIGNUP");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();
        assertEquals("noreply@test.com", msg.getFrom());
        assertArrayEquals(new String[]{"user@test.com"}, msg.getTo());
        assertTrue(msg.getSubject().contains("Account Verification"));
        assertTrue(msg.getText().contains("123456"));
        assertTrue(msg.getText().contains("5 minutes"));
    }

    @Test
    void sendOtpEmail_reset_subject() {
        emailService.sendOtpEmail("user@test.com", "654321", "RESET");
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertTrue(captor.getValue().getSubject().contains("Password Reset"));
    }

    @Test
    void sendOtpEmail_login_defaultSubject() {
        emailService.sendOtpEmail("user@test.com", "111111", "LOGIN");
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertTrue(captor.getValue().getSubject().contains("Login OTP"));
    }

    @Test
    void sendOtpEmail_sendFails_swallowsException() {
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));
        // Must not propagate
        assertDoesNotThrow(() -> emailService.sendOtpEmail("user@test.com", "123456", "LOGIN"));
    }
}
