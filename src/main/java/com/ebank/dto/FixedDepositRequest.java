package com.ebank.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FixedDepositRequest {

    @NotNull(message = "Member ID is required")
    private Long memberId;

    private Long linkedSbId;

    @NotNull(message = "Principal amount is required")
    @DecimalMin(value = "1000.00", message = "Minimum FD amount is 1000")
    private BigDecimal principal;

    @NotNull(message = "Interest rate is required")
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "20.0")
    private BigDecimal interestRate;

    @NotNull(message = "Tenure is required")
    @Min(value = 1, message = "Minimum tenure is 1 month")
    @Max(value = 120, message = "Maximum tenure is 120 months")
    private Integer tenureMonths;

    @Builder.Default
    private String interestType = "SIMPLE";
}