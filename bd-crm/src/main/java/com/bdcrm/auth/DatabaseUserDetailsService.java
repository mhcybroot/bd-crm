package com.bdcrm.auth;

import com.bdcrm.organization.OrganizationStatus;
import com.bdcrm.user.RoleName;
import com.bdcrm.user.User;
import com.bdcrm.user.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        List<User> usernameMatches = userRepository.findAllByUsernameIgnoreCase(username);
        if (usernameMatches.size() == 1) {
            return toAuthenticatedUser(usernameMatches.getFirst());
        }
        List<User> emailMatches = userRepository.findAllByEmailIgnoreCase(username);
        if (emailMatches.size() == 1) {
            return toAuthenticatedUser(emailMatches.getFirst());
        }
        if (usernameMatches.size() > 1 || emailMatches.size() > 1) {
            throw new UsernameNotFoundException("Login identifier is ambiguous; use a unique email");
        }
        throw new UsernameNotFoundException("User not found: " + username);
    }

    public void assertLoginAllowed(String identifier) {
        findExactUser(identifier).ifPresent(this::ensureOrganizationAccessAllowed);
    }

    private java.util.Optional<User> findExactUser(String identifier) {
        List<User> usernameMatches = userRepository.findAllByUsernameIgnoreCase(identifier);
        if (usernameMatches.size() == 1) {
            return java.util.Optional.of(usernameMatches.getFirst());
        }
        List<User> emailMatches = userRepository.findAllByEmailIgnoreCase(identifier);
        if (emailMatches.size() == 1) {
            return java.util.Optional.of(emailMatches.getFirst());
        }
        return java.util.Optional.empty();
    }

    private AuthenticatedUser toAuthenticatedUser(User user) {
        ensureOrganizationAccessAllowed(user);
        return AuthenticatedUser.from(user);
    }

    private void ensureOrganizationAccessAllowed(User user) {
        boolean isPlatformAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.PLATFORM_ADMIN);
        if (!isPlatformAdmin && user.getOrganization().getStatus() != OrganizationStatus.ACTIVE) {
            throw new LockedException(switch (user.getOrganization().getStatus()) {
                case SUSPENDED -> "Your organization is temporarily blocked";
                case ARCHIVED -> "Your organization is archived";
                case ACTIVE -> "Your organization is not active";
            });
        }
    }
}
