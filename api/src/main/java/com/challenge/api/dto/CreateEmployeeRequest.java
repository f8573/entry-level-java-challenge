package com.challenge.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record CreateEmployeeRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotNull @Min(1) Integer salary,
        @NotNull @Min(16) @Max(120) Integer age,
        @NotBlank String jobTitle,
        @NotBlank @Email String email,
        @NotNull Instant contractHireDate,
        Instant contractTerminationDate) {}
