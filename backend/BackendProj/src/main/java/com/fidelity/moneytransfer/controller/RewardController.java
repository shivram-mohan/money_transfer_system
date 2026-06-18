package com.fidelity.moneytransfer.controller;

import com.fidelity.moneytransfer.dto.RewardResponse;
import com.fidelity.moneytransfer.service.RewardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Read access to a user's rewards standing, used by the profile/dashboard tier
 * card. Earning happens implicitly as part of a transfer, so there is no
 * "earn" endpoint here.
 */
@RestController
@RequestMapping("/api/v1/rewards")
@RequiredArgsConstructor
@Slf4j
public class RewardController {

    private final RewardService rewardService;

    @GetMapping("/{accountId}")
    public ResponseEntity<RewardResponse> getRewards(@PathVariable Long accountId) {
        log.info("Fetching rewards for account: {}", accountId);
        return ResponseEntity.ok(rewardService.getRewards(accountId));
    }
}
