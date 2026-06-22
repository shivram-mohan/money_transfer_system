package com.fidelity.moneytransfer.config;

import com.fidelity.moneytransfer.dto.ErrorResponse;
import com.fidelity.moneytransfer.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private WebRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/v1/test");
    }

    @Test
    void accountNotFound_404() {
        ResponseEntity<ErrorResponse> r = handler.handleAccountNotFoundException(
                new AccountNotFoundException("missing"), request);
        assertEquals(HttpStatus.NOT_FOUND, r.getStatusCode());
        assertEquals("ACC-404", r.getBody().getErrorCode());
        assertEquals("/api/v1/test", r.getBody().getPath());
    }

    @Test
    void accountNotActive_403() {
        ResponseEntity<ErrorResponse> r = handler.handleAccountNotActiveException(
                new AccountNotActiveException("locked"), request);
        assertEquals(HttpStatus.FORBIDDEN, r.getStatusCode());
        assertEquals("ACC-403", r.getBody().getErrorCode());
    }

    @Test
    void insufficientBalance_400() {
        ResponseEntity<ErrorResponse> r = handler.handleInsufficientBalanceException(
                new InsufficientBalanceException("no money"), request);
        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
        assertEquals("TRX-400", r.getBody().getErrorCode());
    }

    @Test
    void duplicateTransfer_409() {
        ResponseEntity<ErrorResponse> r = handler.handleDuplicateTransferException(
                new DuplicateTransferException("dup"), request);
        assertEquals(HttpStatus.CONFLICT, r.getStatusCode());
        assertEquals("TRX-409", r.getBody().getErrorCode());
    }

    @Test
    void validation_422_WithFieldErrors() {
        FieldError fieldError = new FieldError("obj", "amount", "must be positive");
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult br = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(br);
        when(br.getFieldErrors()).thenReturn(java.util.List.of(fieldError));

        ResponseEntity<ErrorResponse> r = handler.handleValidationException(ex, request);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, r.getStatusCode());
        assertEquals("VAL-422", r.getBody().getErrorCode());
        assertTrue(r.getBody().getMessage().contains("amount"));
    }

    @Test
    void validation_422_MultipleFieldErrors_AreJoined() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult br = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(br);
        when(br.getFieldErrors()).thenReturn(java.util.List.of(
                new FieldError("obj", "amount", "must be positive"),
                new FieldError("obj", "toAccountId", "is required")));

        ResponseEntity<ErrorResponse> r = handler.handleValidationException(ex, request);

        String msg = r.getBody().getMessage();
        assertTrue(msg.contains("amount"));
        assertTrue(msg.contains("toAccountId"));
        assertTrue(msg.contains(", ")); // reduce joined them
    }

    @Test
    void validation_422_NoFieldErrors_FallsBackToDefaultMessage() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult br = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(br);
        when(br.getFieldErrors()).thenReturn(java.util.List.of());

        ResponseEntity<ErrorResponse> r = handler.handleValidationException(ex, request);

        assertEquals("Validation failed", r.getBody().getMessage());
    }

    @Test
    void illegalArgument_422() {
        ResponseEntity<ErrorResponse> r = handler.handleIllegalArgumentException(
                new IllegalArgumentException("bad"), request);
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, r.getStatusCode());
        assertEquals("VAL-422", r.getBody().getErrorCode());
        assertEquals("bad", r.getBody().getMessage());
    }

    @Test
    void generic_500() {
        ResponseEntity<ErrorResponse> r = handler.handleGlobalException(
                new RuntimeException("boom"), request);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, r.getStatusCode());
        assertEquals("SYS-500", r.getBody().getErrorCode());
    }

    @Test
    void badCredentials_401() {
        ResponseEntity<ErrorResponse> r = handler.handleBadCredentialsException(
                new BadCredentialsException("nope"), request);
        assertEquals(HttpStatus.UNAUTHORIZED, r.getStatusCode());
        assertEquals("AUTH-401", r.getBody().getErrorCode());
        assertEquals("Invalid username or password", r.getBody().getMessage());
    }
}
