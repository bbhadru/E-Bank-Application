package com.ebank.dto;

import lombok.*;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportFilter {
    private LocalDate fromDate;
    private LocalDate toDate;
    private Long memberId;
    private Long accountId;
    private String accountType;
    private String status;
    private String reportType;
}