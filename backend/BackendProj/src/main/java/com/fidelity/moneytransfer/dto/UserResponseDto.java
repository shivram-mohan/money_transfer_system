package com.fidelity.moneytransfer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDto {
    private Long id;
    private String username;
    private String name;
    private String role;
    private String status;
    private Long accountId;
    private String email;
    private LocalDateTime createdDate;
    private String approvedBy;
    private LocalDateTime approvedDate;
}