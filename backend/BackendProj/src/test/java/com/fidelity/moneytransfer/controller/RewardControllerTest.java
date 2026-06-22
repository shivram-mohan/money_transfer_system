package com.fidelity.moneytransfer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fidelity.moneytransfer.config.GlobalExceptionHandler;
import com.fidelity.moneytransfer.dto.RedeemRequest;
import com.fidelity.moneytransfer.dto.RedeemResponse;
import com.fidelity.moneytransfer.dto.RewardProfileResponse;
import com.fidelity.moneytransfer.service.RewardService;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RewardControllerTest {

    @Mock private RewardService rewardService;
    @InjectMocks private RewardController controller;

    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void myRewards_ok() throws Exception {
        when(rewardService.getProfile("john")).thenReturn(
                RewardProfileResponse.builder().holderName("John").pointsBalance(250L)
                        .lifetimePoints(250L).redeemThreshold(500L).canRedeem(false)
                        .progressPercent(50).weekendBonusActive(false).build());

        mockMvc.perform(get("/api/v1/rewards/me")
                        .principal(new UsernamePasswordAuthenticationToken("john", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pointsBalance").value(250));
    }

    @Test
    void redeem_ok() throws Exception {
        when(rewardService.redeem(eq("john"), eq(500L))).thenReturn(
                RedeemResponse.builder().redeemedPoints(500L)
                        .amountCredited(new BigDecimal("500.00")).remainingPoints(0L)
                        .transactionId("tx").message("done").build());

        RedeemRequest req = new RedeemRequest(500L);
        mockMvc.perform(post("/api/v1/rewards/redeem")
                        .principal(new UsernamePasswordAuthenticationToken("john", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.redeemedPoints").value(500));
    }

    @Test
    void redeem_validationError_422() throws Exception {
        RedeemRequest req = new RedeemRequest(0L); // @Min(1)
        mockMvc.perform(post("/api/v1/rewards/redeem")
                        .principal(new UsernamePasswordAuthenticationToken("john", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity());
    }
}
