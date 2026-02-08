//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.fidelity.moneytransfer.controller;

import com.fidelity.moneytransfer.dto.AccountResponse;
import com.fidelity.moneytransfer.entity.TransactionLog;
import com.fidelity.moneytransfer.service.AccountService;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/accounts"})
public class AccountController {
    private static final Logger log = LoggerFactory.getLogger(AccountController.class);
    private final AccountService accountService;

    @GetMapping({"/{id}"})
    public ResponseEntity<AccountResponse> getAccount(@PathVariable Long id) {
        log.info("Fetching account details for id: {}", id);
        AccountResponse response = this.accountService.getAccountDetails(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping({"/{id}/balance"})
    public ResponseEntity<BigDecimal> getBalance(@PathVariable Long id) {
        log.info("Fetching balance for account id: {}", id);
        BigDecimal balance = this.accountService.getBalance(id);
        return ResponseEntity.ok(balance);
    }

    @GetMapping({"/{id}/transactions"})
    public ResponseEntity<List<TransactionLog>> getTransactions(@PathVariable Long id) {
        log.info("Fetching transactions for account id: {}", id);
        List<TransactionLog> transactions = this.accountService.getTransactionHistory(id);
        return ResponseEntity.ok(transactions);
    }

    public AccountController(final AccountService accountService) {
        this.accountService = accountService;
    }
}
