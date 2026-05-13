package com.bdcrm.auth;

import java.util.Set;

public record AuthResponse(
        Long id,
        String username,
        String fullName,
        String email,
        Set<String> roles) {

    public static AuthResponse from(AuthenticatedUser user) {
        return new AuthResponse(
                user.id(),
                user.getUsername(),
                user.fullName(),
                user.email(),
                user.roles());
    }
}
