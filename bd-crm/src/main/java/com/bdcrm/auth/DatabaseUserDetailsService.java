package com.bdcrm.auth;

import com.bdcrm.user.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
        List<com.bdcrm.user.User> usernameMatches = userRepository.findAllByUsernameIgnoreCase(username);
        if (usernameMatches.size() == 1) {
            return AuthenticatedUser.from(usernameMatches.getFirst());
        }
        List<com.bdcrm.user.User> emailMatches = userRepository.findAllByEmailIgnoreCase(username);
        if (emailMatches.size() == 1) {
            return AuthenticatedUser.from(emailMatches.getFirst());
        }
        if (usernameMatches.size() > 1 || emailMatches.size() > 1) {
            throw new UsernameNotFoundException("Login identifier is ambiguous; use a unique email");
        }
        throw new UsernameNotFoundException("User not found: " + username);
    }
}
