//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.fidelity.moneytransfer.service;

import com.fidelity.moneytransfer.dto.AccountResponse;
import com.fidelity.moneytransfer.entity.Account;
import com.fidelity.moneytransfer.entity.TransactionLog;
import com.fidelity.moneytransfer.exception.AccountNotFoundException;
import com.fidelity.moneytransfer.repository.AccountRepository;
import com.fidelity.moneytransfer.repository.TransactionLogRepository;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(
        readOnly = true
)
public class AccountService {
    private static final Logger log = LoggerFactory.getLogger(AccountService.class);
    private final AccountRepository accountRepository;
    private final TransactionLogRepository transactionLogRepository;

    public Account getAccountById(Long id) {
        log.debug("Fetching account with id: {}", id);
        return (Account)this.accountRepository.findById(id).orElseThrow(() -> {
            return new AccountNotFoundException("Account not found with id: " + id);
        });
    }

    public AccountResponse getAccountDetails(Long id) {
        Account account = this.getAccountById(id);
        return this.mapToAccountResponse(account);
    }

    public BigDecimal getBalance(Long id) {
        Account account = this.getAccountById(id);
        return account.getBalance();
    }

    public List<TransactionLog> getTransactionHistory(Long accountId) {
        log.debug("Fetching transaction history for account: {}", accountId);
        this.getAccountById(accountId);
        return this.transactionLogRepository.findByFromAccountIdOrToAccountId(accountId, accountId);
    }

    private AccountResponse mapToAccountResponse(Account account) {
        return AccountResponse.builder().id(account.getId()).holderName(account.getHolderName()).balance(account.getBalance()).status(account.getStatus().name()).lastUpdated(account.getLastUpdated()).build();
    }

    public AccountService(final AccountRepository accountRepository, final TransactionLogRepository transactionLogRepository) {
        this.accountRepository = accountRepository;
        this.transactionLogRepository = transactionLogRepository;
    }
}
