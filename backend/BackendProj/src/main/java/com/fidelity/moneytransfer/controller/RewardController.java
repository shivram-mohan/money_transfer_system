package com.fidelity.moneytransfer.controller;

import com.fidelity.moneytransfer.dto.MonthlySummaryResponse;
import com.fidelity.moneytransfer.dto.RewardProfileResponse;
import com.fidelity.moneytransfer.service.RewardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/v1/rewards")
@RequiredArgsConstructor
@Slf4j
public class RewardController {

    private final RewardService rewardService;

    /** The logged-in user's current rewards standing (points, tier, progress). */
    @GetMapping("/me")
    public ResponseEntity<RewardProfileResponse> myRewards(Authentication authentication) {
        String username = authentication.getName();
        log.debug("Fetching reward profile for {}", username);
        return ResponseEntity.ok(rewardService.getProfile(username));
    }

    /** Reward activity for a given month (defaults to the current month). */
    @GetMapping("/me/summary")
    public ResponseEntity<MonthlySummaryResponse> mySummary(
            Authentication authentication,
            @RequestParam(value = "month", required = false)
            @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        String username = authentication.getName();
        YearMonth target = month != null ? month : YearMonth.now();
        return ResponseEntity.ok(rewardService.getMonthlySummary(username, target));
    }
}
