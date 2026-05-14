package com.bdcrm.auth;

import com.bdcrm.common.ApiException;
import com.bdcrm.organization.Organization;
import com.bdcrm.organization.OrganizationStatus;
import com.bdcrm.organization.OrganizationRepository;
import com.bdcrm.user.User;
import com.bdcrm.user.UserRepository;
import java.util.Arrays;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    public User currentUserEntity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthenticated request");
        }
        User currentUser = userRepository.findById(user.id())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Authenticated user no longer exists"));
        if (currentUser.getOrganization().getStatus() != OrganizationStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Organization is not active");
        }
        return currentUser;
    }

    public Long currentOrganizationId() {
        return currentUserEntity().getOrganization().getId();
    }

    public Organization currentOrganizationEntity() {
        return organizationRepository.findById(currentOrganizationId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Organization no longer exists"));
    }

    public boolean hasPlatformRole(String... roleNames) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return false;
        }
        Set<String> roles = user.platformRoles();
        return Arrays.stream(roleNames).anyMatch(roles::contains);
    }

    public boolean hasOrganizationRole(String... roleNames) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return false;
        }
        if (hasPlatformRole("PLATFORM_ADMIN")) {
            return true;
        }
        Set<String> roles = user.organizationRoles();
        return Arrays.stream(roleNames).anyMatch(roles::contains);
    }

    public boolean hasAnyRole(String... roleNames) {
        return hasPlatformRole(roleNames) || hasOrganizationRole(roleNames);
    }
}
