package com.fidelity.moneytransfer.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Returns a 401 (instead of Spring Security's stateless default of 403) when a
 * request reaches a protected endpoint without a valid token — e.g. because the
 * short-lived access token has expired. The 401 is what the frontend interceptor
 * keys off to transparently refresh the token and retry the request.
 */
@Component
@Slf4j
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        log.debug("Unauthenticated request to {}: {}",
                request.getRequestURI(), authException.getMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"errorCode\":\"AUTH-401\","
                        + "\"message\":\"Authentication required or token expired\"}");
    }
}
