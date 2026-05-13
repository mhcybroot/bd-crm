package com.bdcrm.user;

import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record UserRoleUpdateRequest(@NotEmpty Set<RoleName> roles) {
}
