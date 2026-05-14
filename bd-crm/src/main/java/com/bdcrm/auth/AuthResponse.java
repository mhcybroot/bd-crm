package com.bdcrm.auth;

import java.util.Set;

public record AuthResponse(
        Long id,
        String username,
        String fullName,
        String email,
        Long organizationId,
        String organizationName,
        String organizationSlug,
        Set<String> platformRoles,
        Set<String> organizationRoles) {

    public static AuthResponse from(AuthenticatedUser user) {
        return new AuthResponse(
                user.id(),
                user.getUsername(),
                user.fullName(),
                user.email(),
                user.organizationId(),
                user.organizationName(),
                user.organizationSlug(),
                user.platformRoles(),
                user.organizationRoles());
    }
}
