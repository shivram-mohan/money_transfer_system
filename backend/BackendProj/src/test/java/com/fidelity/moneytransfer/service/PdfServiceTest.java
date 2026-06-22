package com.fidelity.moneytransfer.service;

import com.fidelity.moneytransfer.entity.Account;
import com.fidelity.moneytransfer.entity.TransactionLog;
import com.fidelity.moneytransfer.enums.AccountStatus;
import com.fidelity.moneytransfer.enums.TransactionStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PdfServiceTest {

    private final PdfService pdfService = new PdfService();

    private Account account() {
        return Account.builder().id(100L).holderName("John Doe")
                .balance(new BigDecimal("1000.00")).status(AccountStatus.ACTIVE).version(0).build();
    }

    @Test
    void generate_withDebitAndCredit_andHolderNames() {
        TransactionLog debit = TransactionLog.builder()
                .id("t1").fromAccountId(100L).toAccountId(200L)
                .amount(new BigDecimal("50.00")).status(TransactionStatus.SUCCESS)
                .createdOn(LocalDateTime.now()).toAccountHolderName("Jane").build();
        TransactionLog credit = TransactionLog.builder()
                .id("t2").fromAccountId(300L).toAccountId(100L)
                .amount(new BigDecimal("75.00")).status(TransactionStatus.SUCCESS)
                .createdOn(LocalDateTime.now()).fromAccountHolderName("Bob").build();

        byte[] pdf = pdfService.generateTransactionStatement(
                account(), List.of(debit, credit),
                java.time.LocalDate.now().minusDays(7), java.time.LocalDate.now());

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void generate_withNullCounterpartyNames_usesAccountNumberFallback() {
        TransactionLog debit = TransactionLog.builder()
                .id("t1").fromAccountId(100L).toAccountId(200L)
                .amount(new BigDecimal("50.00")).status(TransactionStatus.SUCCESS)
                .createdOn(LocalDateTime.now()).build(); // no holder names
        TransactionLog credit = TransactionLog.builder()
                .id("t2").fromAccountId(300L).toAccountId(100L)
                .amount(new BigDecimal("75.00")).status(TransactionStatus.SUCCESS)
                .createdOn(LocalDateTime.now()).build();

        byte[] pdf = pdfService.generateTransactionStatement(
                account(), List.of(debit, credit),
                java.time.LocalDate.now().minusDays(7), java.time.LocalDate.now());

        assertTrue(pdf.length > 0);
    }

    @Test
    void generate_withNullAccountIds_handlesNullBranches() {
        // A transaction with null from/to account ids exercises the != null
        // false branches in the summary and table rendering.
        TransactionLog nullIds = TransactionLog.builder()
                .id("t1").fromAccountId(null).toAccountId(null)
                .amount(new BigDecimal("10.00")).status(TransactionStatus.SUCCESS)
                .createdOn(LocalDateTime.now()).build();

        byte[] pdf = pdfService.generateTransactionStatement(
                account(), List.of(nullIds),
                java.time.LocalDate.now().minusDays(7), java.time.LocalDate.now());

        assertTrue(pdf.length > 0);
    }

    @Test
    void generate_emptyTransactions_producesPdf() {
        byte[] pdf = pdfService.generateTransactionStatement(
                account(), Collections.emptyList(),
                java.time.LocalDate.now().minusDays(7), java.time.LocalDate.now());
        assertTrue(pdf.length > 0);
    }

    @Test
    void generate_errorDuringBuild_wrapsInRuntimeException() {
        // A transaction missing its createdOn timestamp triggers an NPE while
        // rendering the table (inside the try block), which is wrapped.
        TransactionLog broken = TransactionLog.builder()
                .id("t1").fromAccountId(100L).toAccountId(200L)
                .amount(new BigDecimal("50.00")).status(TransactionStatus.SUCCESS)
                .createdOn(null).build();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> pdfService.generateTransactionStatement(
                        account(), List.of(broken),
                        java.time.LocalDate.now(), java.time.LocalDate.now()));
        assertEquals("Failed to generate PDF", ex.getMessage());
    }
}
