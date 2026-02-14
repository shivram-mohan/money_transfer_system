package com.fidelity.moneytransfer.controller;

import com.fidelity.moneytransfer.dto.AccountResponse;
import com.fidelity.moneytransfer.dto.CreateAccountRequest;
import com.fidelity.moneytransfer.entity.Account;
import com.fidelity.moneytransfer.entity.TransactionLog;
import com.fidelity.moneytransfer.service.AccountService;
import com.fidelity.moneytransfer.service.PdfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountService accountService;
    private final PdfService pdfService;

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

    @GetMapping("/{id}/transactions/filter")
    public ResponseEntity<List<TransactionLog>> getFilteredTransactions(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate) {

        log.info("Fetching filtered transactions for account: {} from {} to {}",
                id, startDate, endDate);
        return ResponseEntity.ok(
                accountService.getFilteredTransactionHistory(id, startDate, endDate)
        );
    }

    /**
     * Get last week transactions
     */
    @GetMapping("/{id}/transactions/last-week")
    public ResponseEntity<List<TransactionLog>> getLastWeekTransactions(
            @PathVariable Long id) {
        log.info("Fetching last week transactions for account: {}", id);
        return ResponseEntity.ok(accountService.getLastWeekTransactions(id));
    }

    /**
     * Get last month transactions
     */
    @GetMapping("/{id}/transactions/last-month")
    public ResponseEntity<List<TransactionLog>> getLastMonthTransactions(
            @PathVariable Long id) {
        log.info("Fetching last month transactions for account: {}", id);
        return ResponseEntity.ok(accountService.getLastMonthTransactions(id));
    }

    /**
     * Get last year transactions
     */
    @GetMapping("/{id}/transactions/last-year")
    public ResponseEntity<List<TransactionLog>> getLastYearTransactions(
            @PathVariable Long id) {
        log.info("Fetching last year transactions for account: {}", id);
        return ResponseEntity.ok(accountService.getLastYearTransactions(id));
    }

    /**
     * Download PDF statement
     */
    @GetMapping("/{id}/statement/pdf")
    public ResponseEntity<byte[]> downloadPdfStatement(
            @PathVariable Long id,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Generating PDF statement for account: {}", id);

        // Default to last month if dates not provided
        if (startDate == null) {
            startDate = LocalDate.now().minusMonths(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        Account account = accountService.getAccountById(id);
        List<TransactionLog> transactions =
                accountService.getFilteredTransactionHistory(id, startDate, endDate);

        byte[] pdfBytes = pdfService.generateTransactionStatement(
                account, transactions, startDate, endDate
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData(
                "attachment",
                "statement_" + id + "_" + startDate + "_to_" + endDate + ".pdf"
        );

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}