package com.fidelity.moneytransfer.controller;

import com.fidelity.moneytransfer.dto.AccountResponse;
import com.fidelity.moneytransfer.dto.CreateAccountRequest;
import com.fidelity.moneytransfer.entity.Account;
import com.fidelity.moneytransfer.entity.TransactionLog;
import com.fidelity.moneytransfer.enums.AccountStatus;
import com.fidelity.moneytransfer.service.AccountService;
import com.fidelity.moneytransfer.service.PdfService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock private AccountService accountService;
    @Mock private PdfService pdfService;
    @InjectMocks private AccountController controller;

    private AccountResponse sampleResponse() {
        return AccountResponse.builder().id(1L).holderName("John")
                .balance(new BigDecimal("100.00")).status("ACTIVE").build();
    }

    @Test
    void getAccount_ReturnsDetails() {
        when(accountService.getAccountDetails(1L)).thenReturn(sampleResponse());
        ResponseEntity<AccountResponse> r = controller.getAccount(1L);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals("John", r.getBody().getHolderName());
    }

    @Test
    void getBalance_ReturnsBalance() {
        when(accountService.getBalance(1L)).thenReturn(new BigDecimal("250.00"));
        assertEquals(new BigDecimal("250.00"), controller.getBalance(1L).getBody());
    }

    @Test
    void getTransactions_ReturnsList() {
        when(accountService.getTransactionHistory(1L))
                .thenReturn(List.of(new TransactionLog()));
        assertEquals(1, controller.getTransactions(1L).getBody().size());
    }

    @Test
    void getAllAccounts_ReturnsList() {
        when(accountService.getAllAccounts()).thenReturn(List.of(sampleResponse()));
        assertEquals(1, controller.getAllAccounts().getBody().size());
    }

    @Test
    void createAccount_Returns201() {
        CreateAccountRequest req = CreateAccountRequest.builder()
                .holderName("New").initialBalance(new BigDecimal("10.00")).build();
        when(accountService.createAccount(req)).thenReturn(sampleResponse());

        ResponseEntity<AccountResponse> r = controller.createAccount(req);
        assertEquals(HttpStatus.CREATED, r.getStatusCode());
    }

    @Test
    void activateAndDeactivate() {
        when(accountService.activateAccount(1L)).thenReturn(sampleResponse());
        when(accountService.deactivateAccount(1L)).thenReturn(sampleResponse());
        assertEquals(HttpStatus.OK, controller.activateAccount(1L).getStatusCode());
        assertEquals(HttpStatus.OK, controller.deactivateAccount(1L).getStatusCode());
    }

    @Test
    void filteredAndPeriodTransactions() {
        when(accountService.getFilteredTransactionHistory(eq(1L), any(), any()))
                .thenReturn(List.of());
        when(accountService.getLastWeekTransactions(1L)).thenReturn(List.of());
        when(accountService.getLastMonthTransactions(1L)).thenReturn(List.of());
        when(accountService.getLastYearTransactions(1L)).thenReturn(List.of());

        assertNotNull(controller.getFilteredTransactions(
                1L, LocalDate.now().minusDays(7), LocalDate.now()).getBody());
        assertNotNull(controller.getLastWeekTransactions(1L).getBody());
        assertNotNull(controller.getLastMonthTransactions(1L).getBody());
        assertNotNull(controller.getLastYearTransactions(1L).getBody());
    }

    @Test
    void downloadPdf_WithExplicitDates() {
        Account account = Account.builder().id(1L).holderName("John")
                .balance(new BigDecimal("100.00")).status(AccountStatus.ACTIVE)
                .version(0).build();
        when(accountService.getAccountById(1L)).thenReturn(account);
        when(accountService.getFilteredTransactionHistory(eq(1L), any(), any()))
                .thenReturn(List.of());
        when(pdfService.generateTransactionStatement(any(), any(), any(), any()))
                .thenReturn(new byte[]{1, 2, 3});

        ResponseEntity<byte[]> r = controller.downloadPdfStatement(
                1L, LocalDate.now().minusMonths(1), LocalDate.now());

        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals(MediaType.APPLICATION_PDF, r.getHeaders().getContentType());
        assertArrayEquals(new byte[]{1, 2, 3}, r.getBody());
    }

    @Test
    void downloadPdf_DefaultsDatesWhenNull() {
        Account account = Account.builder().id(1L).holderName("John")
                .balance(new BigDecimal("100.00")).status(AccountStatus.ACTIVE)
                .version(0).build();
        when(accountService.getAccountById(1L)).thenReturn(account);
        when(accountService.getFilteredTransactionHistory(eq(1L), any(), any()))
                .thenReturn(List.of());
        when(pdfService.generateTransactionStatement(any(), any(), any(), any()))
                .thenReturn(new byte[]{9});

        ResponseEntity<byte[]> r = controller.downloadPdfStatement(1L, null, null);

        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertArrayEquals(new byte[]{9}, r.getBody());
    }
}
