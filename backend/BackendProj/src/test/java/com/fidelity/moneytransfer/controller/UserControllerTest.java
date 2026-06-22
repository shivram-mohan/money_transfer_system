package com.fidelity.moneytransfer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fidelity.moneytransfer.config.GlobalExceptionHandler;
import com.fidelity.moneytransfer.dto.DeactivateUserRequest;
import com.fidelity.moneytransfer.dto.LinkBankRequest;
import com.fidelity.moneytransfer.dto.LinkBankResponse;
import com.fidelity.moneytransfer.dto.UserResponseDto;
import com.fidelity.moneytransfer.exception.AccountNotFoundException;
import com.fidelity.moneytransfer.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock private UserService userService;
    @InjectMocks private UserController controller;

    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getAllUsers_ok() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(
                UserResponseDto.builder().id(1L).username("john").build()));
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("john"));
    }

    @Test
    void getUserById_ok() throws Exception {
        when(userService.getUserById(1L)).thenReturn(
                UserResponseDto.builder().id(1L).username("john").build());
        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isOk());
    }

    @Test
    void getUserById_notFound_404() throws Exception {
        when(userService.getUserById(9L)).thenThrow(new AccountNotFoundException("no"));
        mockMvc.perform(get("/api/v1/users/9"))
                .andExpect(status().isNotFound());
    }

    @Test
    void linkBank_ok() throws Exception {
        when(userService.linkBankAccount(any(), any())).thenReturn(
                LinkBankResponse.builder().accountId(500L).holderName("John")
                        .encryptedBalance("ENC(100.00)").message("linked").build());

        LinkBankRequest req = new LinkBankRequest(500L);
        mockMvc.perform(post("/api/v1/users/link-bank")
                        .principal(new UsernamePasswordAuthenticationToken("john", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(500));
    }

    @Test
    void activateUser_ok() throws Exception {
        when(userService.activateUser(1L)).thenReturn(
                UserResponseDto.builder().id(1L).status("ACTIVE").build());
        mockMvc.perform(put("/api/v1/users/1/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void deactivateUser_ok() throws Exception {
        when(userService.deactivateUser(any())).thenReturn(
                UserResponseDto.builder().id(1L).status("INACTIVE").build());
        DeactivateUserRequest req = new DeactivateUserRequest(1L, "reason");
        mockMvc.perform(put("/api/v1/users/deactivate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }
}
