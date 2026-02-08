//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.fidelity.moneytransfer.config;

import com.fidelity.moneytransfer.dto.ErrorResponse;
import com.fidelity.moneytransfer.exception.AccountNotActiveException;
import com.fidelity.moneytransfer.exception.AccountNotFoundException;
import com.fidelity.moneytransfer.exception.DuplicateTransferException;
import com.fidelity.moneytransfer.exception.InsufficientBalanceException;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public GlobalExceptionHandler() {
    }

    @ExceptionHandler({AccountNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleAccountNotFoundException(AccountNotFoundException ex, WebRequest request) {
        log.error("Account not found: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder().errorCode("ACC-404").message(ex.getMessage()).timestamp(LocalDateTime.now()).path(request.getDescription(false).replace("uri=", "")).build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler({AccountNotActiveException.class})
    public ResponseEntity<ErrorResponse> handleAccountNotActiveException(AccountNotActiveException ex, WebRequest request) {
        log.error("Account not active: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder().errorCode("ACC-403").message(ex.getMessage()).timestamp(LocalDateTime.now()).path(request.getDescription(false).replace("uri=", "")).build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler({InsufficientBalanceException.class})
    public ResponseEntity<ErrorResponse> handleInsufficientBalanceException(InsufficientBalanceException ex, WebRequest request) {
        log.error("Insufficient balance: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder().errorCode("TRX-400").message(ex.getMessage()).timestamp(LocalDateTime.now()).path(request.getDescription(false).replace("uri=", "")).build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler({DuplicateTransferException.class})
    public ResponseEntity<ErrorResponse> handleDuplicateTransferException(DuplicateTransferException ex, WebRequest request) {
        log.error("Duplicate transfer: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder().errorCode("TRX-409").message(ex.getMessage()).timestamp(LocalDateTime.now()).path(request.getDescription(false).replace("uri=", "")).build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class})
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        log.error("Validation error: {}", ex.getMessage());
        String message = (String)ex.getBindingResult().getFieldErrors().stream().map((errorx) -> {
            String var10000 = errorx.getField();
            return var10000 + ": " + errorx.getDefaultMessage();
        }).reduce((a, b) -> {
            return a + ", " + b;
        }).orElse("Validation failed");
        ErrorResponse error = ErrorResponse.builder().errorCode("VAL-422").message(message).timestamp(LocalDateTime.now()).path(request.getDescription(false).replace("uri=", "")).build();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }

    @ExceptionHandler({IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        log.error("Invalid argument: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder().errorCode("VAL-422").message(ex.getMessage()).timestamp(LocalDateTime.now()).path(request.getDescription(false).replace("uri=", "")).build();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }

    @ExceptionHandler({Exception.class})
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        ErrorResponse error = ErrorResponse.builder().errorCode("SYS-500").message("An unexpected error occurred. Please try again later.").timestamp(LocalDateTime.now()).path(request.getDescription(false).replace("uri=", "")).build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
