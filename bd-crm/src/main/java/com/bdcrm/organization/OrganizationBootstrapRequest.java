package com.bdcrm.organization;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record OrganizationBootstrapRequest(
        @Valid @NotNull OrganizationRequest organization,
        @Valid @NotNull OrganizationBootstrapAdminRequest adminUser) {
}
