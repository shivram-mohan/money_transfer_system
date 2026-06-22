package com.fidelity.moneytransfer.controller;

import com.fidelity.moneytransfer.dto.MonthlySummaryResponse;
import com.fidelity.moneytransfer.dto.RewardProfileResponse;
import com.fidelity.moneytransfer.service.RewardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RewardControllerTest {

    @Mock private RewardService rewardService;
    @Mock private Authentication authentication;
    @InjectMocks private RewardController controller;

    @Test
    void myRewards_ReturnsProfileForAuthenticatedUser() {
        when(authentication.getName()).thenReturn("john");
        RewardProfileResponse profile = RewardProfileResponse.builder()
                .holderName("John").totalPoints(100).tier("BRONZE").build();
        when(rewardService.getProfile("john")).thenReturn(profile);

        assertEquals("BRONZE", controller.myRewards(authentication).getBody().getTier());
    }

    @Test
    void mySummary_WithExplicitMonth() {
        when(authentication.getName()).thenReturn("john");
        YearMonth june = YearMonth.of(2026, 6);
        MonthlySummaryResponse summary = MonthlySummaryResponse.builder()
                .month("June 2026").build();
        when(rewardService.getMonthlySummary("john", june)).thenReturn(summary);

        assertEquals("June 2026",
                controller.mySummary(authentication, june).getBody().getMonth());
    }

    @Test
    void mySummary_DefaultsToCurrentMonthWhenNull() {
        when(authentication.getName()).thenReturn("john");
        when(rewardService.getMonthlySummary(eq("john"), eq(YearMonth.now())))
                .thenReturn(MonthlySummaryResponse.builder().month("now").build());

        assertNotNull(controller.mySummary(authentication, null).getBody());
    }
}
