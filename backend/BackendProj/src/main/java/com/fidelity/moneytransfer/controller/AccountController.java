package com.fidelity.moneytransfer.controller;

import com.fidelity.moneytransfer.dto.AccountResponse;
import com.fidelity.moneytransfer.dto.CreateAccountRequest;
import com.fidelity.moneytransfer.entity.TransactionLog;
import com.fidelity.moneytransfer.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountService accountService;

    // ─── EXISTING ENDPOINTS ───────────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable Long id) {
        log.info("Fetching account details for id: {}", id);
        AccountResponse response = accountService.getAccountDetails(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/balance")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable Long id) {
        log.info("Fetching balance for account id: {}", id);
        BigDecimal balance = accountService.getBalance(id);
        return ResponseEntity.ok(balance);
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<TransactionLog>> getTransactions(@PathVariable Long id) {
        log.info("Fetching transactions for account id: {}", id);
        List<TransactionLog> transactions = accountService.getTransactionHistory(id);
        return ResponseEntity.ok(transactions);
    }

    // ─── ADMIN ENDPOINTS ──────────────────────────────────────────────

    // Get ALL accounts (Admin only)
    @GetMapping
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {
        log.info("Admin: Fetching all accounts");
        List<AccountResponse> accounts = accountService.getAllAccounts();
        return ResponseEntity.ok(accounts);
    }

    // Create new account (Admin only)
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @RequestBody CreateAccountRequest request) {
        log.info("Admin: Creating new account for: {}", request.getHolderName());
        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Activate account (Admin only)
    @PutMapping("/{id}/activate")
    public ResponseEntity<AccountResponse> activateAccount(@PathVariable Long id) {
        log.info("Admin: Activating account id: {}", id);
        AccountResponse response = accountService.activateAccount(id);
        return ResponseEntity.ok(response);
    }

    // Deactivate/Lock account (Admin only)
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<AccountResponse> deactivateAccount(@PathVariable Long id) {
        log.info("Admin: Deactivating account id: {}", id);
        AccountResponse response = accountService.deactivateAccount(id);
        return ResponseEntity.ok(response);
    }
}