package com.fidelity.moneytransfer.service;

import com.fidelity.moneytransfer.dto.MonthlySummaryResponse;
import com.fidelity.moneytransfer.entity.AppUser;
import com.fidelity.moneytransfer.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RewardSchedulerServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RewardService rewardService;
    @Mock private EmailService emailService;
    @InjectMocks private RewardSchedulerService scheduler;

    private AppUser eligible(long id, long points, LocalDateTime lastTxn, boolean warned) {
        return AppUser.builder()
                .id(id).username("u" + id).name("User " + id)
                .email("u" + id + "@example.com")
                .role("USER").status("ACTIVE").accountId(2000L + id)
                .rewardPoints(points).tier("X")
                .lastTransactionDate(lastTxn).downgradeWarningSent(warned)
                .build();
    }

    // ─── MONTHLY SUMMARIES ───────────────────────────────────────────

    @Test
    void sendMonthlySummaries_SendsEmailPerEligibleUser() {
        AppUser u = eligible(1, 600, LocalDateTime.now(), false);
        when(userRepository.findByStatus("ACTIVE")).thenReturn(List.of(u));
        when(rewardService.buildMonthlySummary(eq(u), any()))
                .thenReturn(MonthlySummaryResponse.builder()
                        .month("May 2026").rewardedTransfers(3).pointsEarned(30)
                        .cashbackEarned(new BigDecimal("12.00")).tier("SILVER")
                        .totalPoints(600).build());

        scheduler.sendMonthlySummaries();

        verify(emailService).sendMonthlySummaryEmail(
                eq("u1@example.com"), eq("User 1"), eq("May 2026"),
                eq(3L), eq(30L), eq("12.00"), eq("SILVER"), eq(600L));
    }

    @Test
    void sendMonthlySummaries_OneUserFails_BatchContinues() {
        AppUser u = eligible(1, 600, LocalDateTime.now(), false);
        when(userRepository.findByStatus("ACTIVE")).thenReturn(List.of(u));
        when(rewardService.buildMonthlySummary(any(), any()))
                .thenThrow(new RuntimeException("boom"));

        scheduler.sendMonthlySummaries(); // must not throw

        verify(emailService, never()).sendMonthlySummaryEmail(
                any(), any(), any(), anyLong(), anyLong(), any(), any(), anyLong());
    }

    @Test
    void eligibleUsers_FiltersOutUnlinkedAndEmaillessAndInactive() {
        AppUser ok = eligible(1, 100, LocalDateTime.now(), false);
        AppUser noBank = eligible(2, 100, LocalDateTime.now(), false);
        noBank.setAccountId(null);
        AppUser noEmail = eligible(3, 100, LocalDateTime.now(), false);
        noEmail.setEmail(" ");
        when(userRepository.findByStatus("ACTIVE")).thenReturn(List.of(ok, noBank, noEmail));
        when(rewardService.buildMonthlySummary(eq(ok), any()))
                .thenReturn(MonthlySummaryResponse.builder().month("m")
                        .cashbackEarned(BigDecimal.ZERO).tier("BRONZE").build());

        scheduler.sendMonthlySummaries();

        verify(rewardService, times(1)).buildMonthlySummary(any(), any());
    }

    // ─── INACTIVITY SWEEP ────────────────────────────────────────────

    @Test
    void inactivitySweep_NeverTransacted_NoAction() {
        AppUser u = eligible(1, 600, null, false);
        when(userRepository.findByStatus("ACTIVE")).thenReturn(List.of(u));

        scheduler.runInactivitySweep();

        verify(userRepository, never()).save(any());
        verifyNoInteractions(emailService);
    }

    @Test
    void inactivitySweep_NullRewardPoints_TreatedAsBronze_NoDowngrade() {
        AppUser u = eligible(1, 100, LocalDateTime.now().minusDays(40), false);
        u.setRewardPoints(null); // exercises the null-points -> 0 ternary branch
        when(userRepository.findByStatus("ACTIVE")).thenReturn(List.of(u));

        scheduler.runInactivitySweep();

        verify(userRepository, never()).save(any());
    }

    @Test
    void inactivitySweep_BronzeUser_NoDowngrade() {
        AppUser u = eligible(1, 100, LocalDateTime.now().minusDays(60), false);
        when(userRepository.findByStatus("ACTIVE")).thenReturn(List.of(u));

        scheduler.runInactivitySweep();

        verify(userRepository, never()).save(any());
    }

    @Test
    void inactivitySweep_Over30DaysInactive_DowngradesAndEmails() {
        AppUser u = eligible(1, 2000, LocalDateTime.now().minusDays(40), false); // GOLD
        when(userRepository.findByStatus("ACTIVE")).thenReturn(List.of(u));
        when(userRepository.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));

        scheduler.runInactivitySweep();

        verify(emailService).sendDowngradeEmail(
                eq("u1@example.com"), eq("User 1"), eq("GOLD"), eq("SILVER"));
        verify(userRepository).save(u);
    }

    @Test
    void inactivitySweep_WithinWarningWindow_SendsWarningOnce() {
        AppUser u = eligible(1, 600, LocalDateTime.now().minusDays(22), false); // SILVER, 22d
        when(userRepository.findByStatus("ACTIVE")).thenReturn(List.of(u));
        when(userRepository.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));

        scheduler.runInactivitySweep();

        verify(emailService).sendDowngradeWarningEmail(
                eq("u1@example.com"), eq("User 1"), eq("SILVER"), anyLong());
        verify(userRepository).save(u);
    }

    @Test
    void inactivitySweep_WarningAlreadySent_NoDuplicate() {
        AppUser u = eligible(1, 600, LocalDateTime.now().minusDays(22), true);
        when(userRepository.findByStatus("ACTIVE")).thenReturn(List.of(u));

        scheduler.runInactivitySweep();

        verify(emailService, never()).sendDowngradeWarningEmail(any(), any(), any(), anyLong());
        verify(userRepository, never()).save(any());
    }

    @Test
    void inactivitySweep_RecentlyActive_NoAction() {
        AppUser u = eligible(1, 600, LocalDateTime.now().minusDays(5), false);
        when(userRepository.findByStatus("ACTIVE")).thenReturn(List.of(u));

        scheduler.runInactivitySweep();

        verifyNoInteractions(emailService);
        verify(userRepository, never()).save(any());
    }

    @Test
    void inactivitySweep_FailureIsolated_BatchContinues() {
        AppUser u = eligible(1, 2000, LocalDateTime.now().minusDays(40), false);
        when(userRepository.findByStatus("ACTIVE")).thenReturn(List.of(u));
        when(userRepository.save(any(AppUser.class)))
                .thenThrow(new RuntimeException("db down"));

        scheduler.runInactivitySweep(); // must not throw
    }
}
