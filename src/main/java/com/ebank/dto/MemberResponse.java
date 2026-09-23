package com.ebank.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberResponse {
    private Long memberId;
    private String memberCode;
    private String fullName;
    private LocalDate dob;
    private String gender;
    private String mobile;
    private String email;
    private String address;
    private String kycNumber;
    private String status;
    private LocalDateTime createdAt;
    private int savingsAccountsCount;
    private int loansCount;
}