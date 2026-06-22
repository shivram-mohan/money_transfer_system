package com.fidelity.moneytransfer.config;

import com.fidelity.moneytransfer.dto.ErrorResponse;
import com.fidelity.moneytransfer.exception.AccountNotActiveException;
import com.fidelity.moneytransfer.exception.AccountNotFoundException;
import com.fidelity.moneytransfer.exception.DuplicateTransferException;
import com.fidelity.moneytransfer.exception.InsufficientBalanceException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private WebRequest req() {
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/v1/test");
        return request;
    }

    @Test
    void accountNotFound_404() {
        ResponseEntity<ErrorResponse> resp = handler.handleAccountNotFoundException(
                new AccountNotFoundException("missing"), req());
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertEquals("ACC-404", resp.getBody().getErrorCode());
        assertEquals("/api/v1/test", resp.getBody().getPath());
    }

    @Test
    void accountNotActive_403() {
        ResponseEntity<ErrorResponse> resp = handler.handleAccountNotActiveException(
                new AccountNotActiveException("locked"), req());
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
        assertEquals("ACC-403", resp.getBody().getErrorCode());
    }

    @Test
    void insufficientBalance_400() {
        ResponseEntity<ErrorResponse> resp = handler.handleInsufficientBalanceException(
                new InsufficientBalanceException("broke"), req());
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("TRX-400", resp.getBody().getErrorCode());
    }

    @Test
    void duplicateTransfer_409() {
        ResponseEntity<ErrorResponse> resp = handler.handleDuplicateTransferException(
                new DuplicateTransferException("dupe"), req());
        assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
        assertEquals("TRX-409", resp.getBody().getErrorCode());
    }

    @Test
    void validation_withFieldErrors_422() {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "obj");
        binding.addError(new FieldError("obj", "amount", "must be positive"));
        binding.addError(new FieldError("obj", "name", "is required"));
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(binding);

        ResponseEntity<ErrorResponse> resp = handler.handleValidationException(ex, req());
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, resp.getStatusCode());
        assertEquals("VAL-422", resp.getBody().getErrorCode());
        assertTrue(resp.getBody().getMessage().contains("amount"));
        assertTrue(resp.getBody().getMessage().contains(", ")); // reduce() joined two errors
    }

    @Test
    void validation_noFieldErrors_fallbackMessage() {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "obj");
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(binding);
        ResponseEntity<ErrorResponse> resp = handler.handleValidationException(ex, req());
        assertEquals("Validation failed", resp.getBody().getMessage());
    }

    @Test
    void illegalArgument_422() {
        ResponseEntity<ErrorResponse> resp = handler.handleIllegalArgumentException(
                new IllegalArgumentException("bad arg"), req());
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, resp.getStatusCode());
        assertEquals("VAL-422", resp.getBody().getErrorCode());
        assertEquals("bad arg", resp.getBody().getMessage());
    }

    @Test
    void global_500() {
        ResponseEntity<ErrorResponse> resp = handler.handleGlobalException(
                new RuntimeException("boom"), req());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        assertEquals("SYS-500", resp.getBody().getErrorCode());
    }

    @Test
    void badCredentials_401() {
        ResponseEntity<ErrorResponse> resp = handler.handleBadCredentialsException(
                new BadCredentialsException("nope"), req());
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        assertEquals("AUTH-401", resp.getBody().getErrorCode());
        assertEquals("Invalid username or password", resp.getBody().getMessage());
    }
}
