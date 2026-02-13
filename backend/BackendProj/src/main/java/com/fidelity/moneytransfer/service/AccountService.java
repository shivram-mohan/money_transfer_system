package com.fidelity.moneytransfer.service;

import com.fidelity.moneytransfer.dto.AccountResponse;
import com.fidelity.moneytransfer.dto.CreateAccountRequest;
import com.fidelity.moneytransfer.entity.Account;
import com.fidelity.moneytransfer.entity.TransactionLog;
import com.fidelity.moneytransfer.enums.AccountStatus;
import com.fidelity.moneytransfer.exception.AccountNotFoundException;
import com.fidelity.moneytransfer.repository.AccountRepository;
import com.fidelity.moneytransfer.repository.TransactionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionLogRepository transactionLogRepository;

    // ─── EXISTING METHODS ─────────────────────────────────────────────

    public Account getAccountById(Long id) {
        log.debug("Fetching account with id: {}", id);
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found with id: " + id));
    }

    public AccountResponse getAccountDetails(Long id) {
        Account account = getAccountById(id);
        return mapToAccountResponse(account);
    }

    public BigDecimal getBalance(Long id) {
        Account account = getAccountById(id);
        return account.getBalance();
    }

    public List<TransactionLog> getTransactionHistory(Long accountId) {
        log.debug("Fetching transaction history for account: {}", accountId);
        getAccountById(accountId);
        return transactionLogRepository
                .findByFromAccountIdOrToAccountId(accountId, accountId);
    }

    // ─── ADMIN METHODS ────────────────────────────────────────────────

    public List<AccountResponse> getAllAccounts() {
        log.debug("Fetching all accounts");
        return accountRepository.findAll()
                .stream()
                .map(this::mapToAccountResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        log.info("Creating new account for: {}", request.getHolderName());

        Account account = Account.builder()
                .holderName(request.getHolderName())
                .balance(request.getInitialBalance())
                .status(AccountStatus.ACTIVE)
                .version(0)
                .build();

        Account savedAccount = accountRepository.save(account);
        log.info("Account created with id: {}", savedAccount.getId());

        return mapToAccountResponse(savedAccount);
    }

    @Transactional
    public AccountResponse activateAccount(Long id) {
        log.info("Activating account id: {}", id);

        Account account = getAccountById(id);
        account.setStatus(AccountStatus.ACTIVE);
        Account savedAccount = accountRepository.save(account);

        return mapToAccountResponse(savedAccount);
    }

    @Transactional
    public AccountResponse deactivateAccount(Long id) {
        log.info("Deactivating account id: {}", id);

        Account account = getAccountById(id);
        account.setStatus(AccountStatus.LOCKED);
        Account savedAccount = accountRepository.save(account);

        return mapToAccountResponse(savedAccount);
    }

    // ─── HELPER METHODS ───────────────────────────────────────────────

    private AccountResponse mapToAccountResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .holderName(account.getHolderName())
                .balance(account.getBalance())
                .status(account.getStatus().name())
                .lastUpdated(account.getLastUpdated())
                .build();
    }
}