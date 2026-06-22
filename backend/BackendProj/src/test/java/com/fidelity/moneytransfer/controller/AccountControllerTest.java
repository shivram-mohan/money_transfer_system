package com.fidelity.moneytransfer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fidelity.moneytransfer.config.GlobalExceptionHandler;
import com.fidelity.moneytransfer.dto.AccountResponse;
import com.fidelity.moneytransfer.dto.CreateAccountRequest;
import com.fidelity.moneytransfer.entity.Account;
import com.fidelity.moneytransfer.entity.TransactionLog;
import com.fidelity.moneytransfer.enums.AccountStatus;
import com.fidelity.moneytransfer.exception.AccountNotFoundException;
import com.fidelity.moneytransfer.service.AccountService;
import com.fidelity.moneytransfer.service.PdfService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock private AccountService accountService;
    @Mock private PdfService pdfService;
    @InjectMocks private AccountController controller;

    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private AccountResponse resp() {
        return AccountResponse.builder().id(1L).holderName("John")
                .balance(new BigDecimal("100.00")).status("ACTIVE").build();
    }

    @Test
    void getAccount_ok() throws Exception {
        when(accountService.getAccountDetails(1L)).thenReturn(resp());
        mockMvc.perform(get("/api/v1/accounts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holderName").value("John"));
    }

    @Test
    void getAccount_notFound_404() throws Exception {
        when(accountService.getAccountDetails(9L)).thenThrow(new AccountNotFoundException("no"));
        mockMvc.perform(get("/api/v1/accounts/9"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getBalance_returnsCiphertext() throws Exception {
        // Service returns the balance already AES-encrypted; controller passes it through.
        when(accountService.getBalance(1L)).thenReturn("ENC(250.00)");
        mockMvc.perform(get("/api/v1/accounts/1/balance"))
                .andExpect(status().isOk())
                .andExpect(content().string("ENC(250.00)"));
    }

    @Test
    void getTransactions_ok() throws Exception {
        when(accountService.getTransactionHistory(1L)).thenReturn(List.of(new TransactionLog()));
        mockMvc.perform(get("/api/v1/accounts/1/transactions"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllAccounts_ok() throws Exception {
        when(accountService.getAllAccounts()).thenReturn(List.of(resp()));
        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void createAccount_created() throws Exception {
        when(accountService.createAccount(any())).thenReturn(resp());
        CreateAccountRequest req = CreateAccountRequest.builder()
                .holderName("John").initialBalance(new BigDecimal("100.00")).build();
        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    void activateAccount_ok() throws Exception {
        when(accountService.activateAccount(1L)).thenReturn(resp());
        mockMvc.perform(put("/api/v1/accounts/1/activate"))
                .andExpect(status().isOk());
    }

    @Test
    void deactivateAccount_ok() throws Exception {
        AccountResponse locked = resp();
        locked.setStatus("LOCKED");
        when(accountService.deactivateAccount(1L)).thenReturn(locked);
        mockMvc.perform(put("/api/v1/accounts/1/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LOCKED"));
    }

    @Test
    void getFilteredTransactions_ok() throws Exception {
        when(accountService.getFilteredTransactionHistory(eq(1L), any(), any()))
                .thenReturn(List.of());
        mockMvc.perform(get("/api/v1/accounts/1/transactions/filter")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31"))
                .andExpect(status().isOk());
    }

    @Test
    void getLastWeek_ok() throws Exception {
        when(accountService.getLastWeekTransactions(1L)).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/accounts/1/transactions/last-week"))
                .andExpect(status().isOk());
    }

    @Test
    void getLastMonth_ok() throws Exception {
        when(accountService.getLastMonthTransactions(1L)).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/accounts/1/transactions/last-month"))
                .andExpect(status().isOk());
    }

    @Test
    void getLastYear_ok() throws Exception {
        when(accountService.getLastYearTransactions(1L)).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/accounts/1/transactions/last-year"))
                .andExpect(status().isOk());
    }

    @Test
    void downloadPdf_withDates() throws Exception {
        Account account = Account.builder().id(1L).holderName("John")
                .balance(new BigDecimal("100.00")).status(AccountStatus.ACTIVE).build();
        when(accountService.getAccountById(1L)).thenReturn(account);
        when(accountService.getFilteredTransactionHistory(eq(1L), any(), any())).thenReturn(List.of());
        when(pdfService.generateTransactionStatement(any(), any(), any(), any()))
                .thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/v1/accounts/1/statement/pdf")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void downloadPdf_defaultDates() throws Exception {
        Account account = Account.builder().id(1L).holderName("John")
                .balance(new BigDecimal("100.00")).status(AccountStatus.ACTIVE).build();
        when(accountService.getAccountById(1L)).thenReturn(account);
        when(accountService.getFilteredTransactionHistory(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(pdfService.generateTransactionStatement(any(), any(), any(), any()))
                .thenReturn(new byte[]{9});

        mockMvc.perform(get("/api/v1/accounts/1/statement/pdf"))
                .andExpect(status().isOk());
    }
}
