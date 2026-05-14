package com.bdcrm.organization;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record OrganizationBootstrapAdminRequest(
        @NotBlank String username,
        @NotBlank String password,
        @NotBlank String fullName,
        @Email @NotBlank String email) {
}
