package com.fidelity.moneytransfer.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.AuthenticationException;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthEntryPointTest {

    @Test
    void commence_Writes401Json() throws Exception {
        JwtAuthEntryPoint entryPoint = new JwtAuthEntryPoint();

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AuthenticationException ex = mock(AuthenticationException.class);

        when(request.getRequestURI()).thenReturn("/api/v1/accounts/1");
        when(ex.getMessage()).thenReturn("expired");

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        entryPoint.commence(request, response, ex);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        String body = sw.toString();
        assertTrue(body.contains("AUTH-401"));
        assertTrue(body.contains("Authentication required or token expired"));
    }
}
