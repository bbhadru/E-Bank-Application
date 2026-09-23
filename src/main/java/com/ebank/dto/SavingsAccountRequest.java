package com.ebank.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsAccountRequest {

    @NotNull(message = "Member ID is required")
    private Long memberId;

    @NotNull(message = "Interest rate is required")
    @DecimalMin(value = "0.0", message = "Interest rate cannot be negative")
    @DecimalMax(value = "20.0", message = "Interest rate cannot exceed 20%")
    private BigDecimal interestRate;

    private BigDecimal minimumBalance;
}