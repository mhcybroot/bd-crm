package com.bdcrm.organization;

import com.bdcrm.user.UserResponse;

public record OrganizationBootstrapResponse(
        OrganizationResponse organization,
        UserResponse firstAdminUser) {
}
