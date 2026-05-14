package com.bdcrm.auth;

import com.bdcrm.organization.Organization;
import com.bdcrm.user.User;
import java.util.Collection;
import java.util.Set;
import org.hibernate.Hibernate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record AuthenticatedUser(
        Long id,
        String username,
        String password,
        String fullName,
        String email,
        Long organizationId,
        String organizationName,
        String organizationSlug,
        boolean enabled,
        Set<String> platformRoles,
        Set<String> organizationRoles,
        String token) implements UserDetails {

    public static AuthenticatedUser from(User user) {
        Set<String> platformRoles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .filter(role -> role.equals("PLATFORM_ADMIN"))
                .collect(java.util.stream.Collectors.toSet());
        Set<String> organizationRoles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .filter(role -> !role.equals("PLATFORM_ADMIN"))
                .collect(java.util.stream.Collectors.toSet());
        Organization organization = user.getOrganization();
        String organizationName = Hibernate.isInitialized(organization) ? organization.getName() : null;
        String organizationSlug = Hibernate.isInitialized(organization) ? organization.getSlug() : null;
        return new AuthenticatedUser(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getFullName(),
                user.getEmail(),
                organization != null ? organization.getId() : null,
                organizationName,
                organizationSlug,
                user.isActive(),
                platformRoles,
                organizationRoles,
                null);
    }

    public AuthenticatedUser withToken(String token) {
        return new AuthenticatedUser(id, username, password, fullName, email, organizationId, organizationName, organizationSlug, enabled, platformRoles, organizationRoles, token);
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return java.util.stream.Stream.concat(platformRoles.stream(), organizationRoles.stream())
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return enabled;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
