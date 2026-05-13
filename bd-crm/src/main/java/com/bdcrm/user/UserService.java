package com.bdcrm.user;

import com.bdcrm.audit.AuditEventService;
import com.bdcrm.auth.SecurityUtils;
import com.bdcrm.common.ApiException;
import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditEventService auditEventService;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return userRepository.findAllByOrderByFullNameAsc().stream().map(UserResponse::from).toList();
    }

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        if (userRepository.findByUsernameIgnoreCase(request.username()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Username already exists");
        }
        User user = new User();
        user.setUsername(request.username().trim());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setActive(true);
        user.setRoles(new HashSet<>(loadRoles(request.roles())));
        if (request.managerId() != null) {
            user.setManager(requireUser(request.managerId()));
        }
        user = userRepository.save(user);
        auditEventService.log(securityUtils.currentUserEntity(), "USER_CREATED", "USER", user.getId(), "Created user " + user.getUsername(), null);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateRoles(Long userId, UserRoleUpdateRequest request) {
        User user = requireUser(userId);
        user.setRoles(new HashSet<>(loadRoles(request.roles())));
        auditEventService.log(securityUtils.currentUserEntity(), "USER_ROLES_UPDATED", "USER", user.getId(), "Updated user roles", null);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateStatus(Long userId, UserStatusUpdateRequest request) {
        User user = requireUser(userId);
        user.setActive(request.active());
        auditEventService.log(securityUtils.currentUserEntity(), "USER_STATUS_UPDATED", "USER", user.getId(), "Updated user active status", null);
        return UserResponse.from(user);
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private List<Role> loadRoles(java.util.Set<RoleName> roleNames) {
        List<Role> roles = roleRepository.findByNameIn(roleNames);
        if (roles.size() != roleNames.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "One or more roles are invalid");
        }
        return roles;
    }
}
