package com.ebank.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShareCapitalRequest {

    @NotNull(message = "Member ID is required")
    private Long memberId;

    @NotNull(message = "Number of shares is required")
    @Min(value = 1, message = "Minimum 1 share")
    private Integer sharesCount;

    private String narration;
}