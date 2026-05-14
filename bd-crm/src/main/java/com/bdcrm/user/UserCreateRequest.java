package com.bdcrm.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record UserCreateRequest(
        @NotBlank String username,
        @NotBlank String password,
        @NotBlank String fullName,
        @Email @NotBlank String email,
        Long managerId,
        Long organizationId,
        @NotEmpty Set<RoleName> roles) {
}
