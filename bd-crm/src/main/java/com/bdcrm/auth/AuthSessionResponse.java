package com.bdcrm.auth;

public record AuthSessionResponse(
        String token,
        AuthResponse user) {

    public static AuthSessionResponse from(AuthenticatedUser user) {
        return new AuthSessionResponse(user.token(), AuthResponse.from(user));
    }
}
