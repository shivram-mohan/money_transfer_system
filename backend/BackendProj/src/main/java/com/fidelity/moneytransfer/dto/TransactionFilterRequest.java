package com.fidelity.moneytransfer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionFilterRequest {

    private Long accountId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String filterType; // LAST_WEEK, LAST_MONTH, LAST_YEAR, CUSTOM

    public static TransactionFilterRequest lastWeek(Long accountId) {
        LocalDate now = LocalDate.now();
        return TransactionFilterRequest.builder()
                .accountId(accountId)
                .startDate(now.minusWeeks(1))
                .endDate(now)
                .filterType("LAST_WEEK")
                .build();
    }

    public static TransactionFilterRequest lastMonth(Long accountId) {
        LocalDate now = LocalDate.now();
        return TransactionFilterRequest.builder()
                .accountId(accountId)
                .startDate(now.minusMonths(1))
                .endDate(now)
                .filterType("LAST_MONTH")
                .build();
    }

    public static TransactionFilterRequest lastYear(Long accountId) {
        LocalDate now = LocalDate.now();
        return TransactionFilterRequest.builder()
                .accountId(accountId)
                .startDate(now.minusYears(1))
                .endDate(now)
                .filterType("LAST_YEAR")
                .build();
    }

    public static TransactionFilterRequest custom(Long accountId,
                                                  LocalDate startDate,
                                                  LocalDate endDate) {
        return TransactionFilterRequest.builder()
                .accountId(accountId)
                .startDate(startDate)
                .endDate(endDate)
                .filterType("CUSTOM")
                .build();
    }
}