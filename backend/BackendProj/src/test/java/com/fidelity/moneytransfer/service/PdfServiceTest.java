package com.fidelity.moneytransfer.service;

import com.fidelity.moneytransfer.entity.Account;
import com.fidelity.moneytransfer.entity.TransactionLog;
import com.fidelity.moneytransfer.enums.AccountStatus;
import com.fidelity.moneytransfer.enums.TransactionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PdfServiceTest {

    private PdfService pdfService;
    private Account account;

    @BeforeEach
    void setUp() {
        pdfService = new PdfService();
        account = Account.builder()
                .id(1001L).holderName("John Doe")
                .balance(new BigDecimal("5000.00"))
                .status(AccountStatus.ACTIVE).version(0).build();
    }

    @Test
    void generateStatement_WithTransactions_ReturnsPdfBytes() {
        TransactionLog debit = TransactionLog.builder()
                .id("t1").fromAccountId(1001L).toAccountId(2002L)
                .amount(new BigDecimal("200.00")).status(TransactionStatus.SUCCESS)
                .createdOn(LocalDateTime.now()).build();
        debit.setToAccountHolderName("Jane");

        TransactionLog credit = TransactionLog.builder()
                .id("t2").fromAccountId(3003L).toAccountId(1001L)
                .amount(new BigDecimal("500.00")).status(TransactionStatus.SUCCESS)
                .createdOn(LocalDateTime.now()).build();
        // no holder names set -> exercises the "Account #" fallback branch

        byte[] pdf = pdfService.generateTransactionStatement(
                account, List.of(debit, credit),
                LocalDate.now().minusMonths(1), LocalDate.now());

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
        // PDF files start with the "%PDF" magic bytes
        assertEquals('%', pdf[0]);
        assertEquals('P', pdf[1]);
    }

    @Test
    void generateStatement_CounterpartyNameFallbacks() {
        // Debit with NO recipient holder name -> "Account #<id>" fallback
        TransactionLog debitNoName = TransactionLog.builder()
                .id("d").fromAccountId(1001L).toAccountId(2002L)
                .amount(new BigDecimal("75.00")).status(TransactionStatus.SUCCESS)
                .createdOn(LocalDateTime.now()).build();
        // Credit WITH sender holder name present
        TransactionLog creditWithName = TransactionLog.builder()
                .id("c").fromAccountId(3003L).toAccountId(1001L)
                .amount(new BigDecimal("80.00")).status(TransactionStatus.SUCCESS)
                .createdOn(LocalDateTime.now()).build();
        creditWithName.setFromAccountHolderName("Charlie");

        byte[] pdf = pdfService.generateTransactionStatement(
                account, List.of(debitNoName, creditWithName),
                LocalDate.now().minusMonths(1), LocalDate.now());

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void generateStatement_NullCounterpartyIds_Handled() {
        TransactionLog nullIds = TransactionLog.builder()
                .id("n").fromAccountId(null).toAccountId(null)
                .amount(new BigDecimal("5.00")).status(TransactionStatus.SUCCESS)
                .createdOn(LocalDateTime.now()).build();

        byte[] pdf = pdfService.generateTransactionStatement(
                account, List.of(nullIds),
                LocalDate.now().minusWeeks(1), LocalDate.now());

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void generateStatement_EmptyTransactions_StillReturnsPdf() {
        byte[] pdf = pdfService.generateTransactionStatement(
                account, Collections.emptyList(),
                LocalDate.now().minusWeeks(1), LocalDate.now());

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void generateStatement_FailureInGeneration_WrapsInRuntimeException() {
        // A transaction with a null createdOn blows up during table rendering,
        // which is caught and rethrown as a RuntimeException by the service.
        TransactionLog broken = TransactionLog.builder()
                .id("bad").fromAccountId(1001L).toAccountId(2002L)
                .amount(new BigDecimal("10.00")).status(TransactionStatus.SUCCESS)
                .createdOn(null).build();

        assertThrows(RuntimeException.class, () ->
                pdfService.generateTransactionStatement(
                        account, List.of(broken),
                        LocalDate.now().minusWeeks(1), LocalDate.now()));
    }
}
