package com.ebank.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherRequest {

    @NotBlank(message = "Voucher type is required")
    private String voucherType;

    @NotNull(message = "Voucher date is required")
    private LocalDate voucherDate;

    private String narration;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    @NotEmpty(message = "At least one ledger entry is required")
    private List<LedgerEntryRequest> entries;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LedgerEntryRequest {
        @NotNull(message = "Ledger ID is required")
        private Long ledgerId;

        private BigDecimal debit;
        private BigDecimal credit;
        private String narration;
    }
}