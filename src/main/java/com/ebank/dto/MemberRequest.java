package com.ebank.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 150, message = "Full name cannot exceed 150 characters")
    private String fullName;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dob;

    private String gender;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[0-9]{10,15}$", message = "Invalid mobile number")
    private String mobile;

    @Email(message = "Invalid email format")
    private String email;

    private String address;

    private String kycNumber;

    private boolean createSavingsAccount;
}