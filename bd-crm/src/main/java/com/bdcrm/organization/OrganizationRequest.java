package com.bdcrm.organization;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record OrganizationRequest(
        @NotBlank String slug,
        @NotBlank String name,
        OrganizationStatus status,
        @NotBlank String timezone,
        @NotBlank String locale,
        @Email @NotBlank String contactEmail,
        @NotBlank String planCode,
        @Min(1) int dataRetentionDays) {
}
