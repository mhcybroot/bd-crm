package com.bdcrm.user;

import java.util.Set;

public record UserResponse(
        Long id,
        String username,
        String fullName,
        String email,
        boolean active,
        Long managerId,
        Set<String> roles) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.isActive(),
                user.getManager() == null ? null : user.getManager().getId(),
                user.getRoles().stream().map(role -> role.getName().name()).collect(java.util.stream.Collectors.toSet()));
    }
}
