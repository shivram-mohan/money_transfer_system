package com.fidelity.moneytransfer.controller;

import com.fidelity.moneytransfer.dto.RedeemRequest;
import com.fidelity.moneytransfer.dto.RedeemResponse;
import com.fidelity.moneytransfer.dto.RewardProfileResponse;
import com.fidelity.moneytransfer.service.RewardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rewards")
@RequiredArgsConstructor
@Slf4j
public class RewardController {

    private final RewardService rewardService;

    /** The logged-in user's current rewards standing (balance, lifetime, progress). */
    @GetMapping("/me")
    public ResponseEntity<RewardProfileResponse> myRewards(Authentication authentication) {
        return ResponseEntity.ok(rewardService.getProfile(authentication.getName()));
    }

    /** Redeem reward points as cash into the user's bank account. */
    @PostMapping("/redeem")
    public ResponseEntity<RedeemResponse> redeem(
            Authentication authentication,
            @Valid @RequestBody RedeemRequest request) {
        String username = authentication.getName();
        log.info("User {} redeeming {} reward points", username, request.getPoints());
        return ResponseEntity.ok(rewardService.redeem(username, request.getPoints()));
    }
}
