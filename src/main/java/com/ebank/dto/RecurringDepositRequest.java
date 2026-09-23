package com.ebank.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringDepositRequest {

    @NotNull(message = "Member ID is required")
    private Long memberId;

    private Long linkedSbId;

    @NotNull(message = "Monthly amount is required")
    @DecimalMin(value = "100.00", message = "Minimum monthly amount is 100")
    private BigDecimal monthlyAmount;

    @NotNull(message = "Tenure is required")
    @Min(value = 6, message = "Minimum tenure is 6 months")
    @Max(value = 120, message = "Maximum tenure is 120 months")
    private Integer tenureMonths;

    @NotNull(message = "Interest rate is required")
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "20.0")
    private BigDecimal interestRate;
}