package com.bdcrm.user;

import com.bdcrm.audit.AuditEventService;
import com.bdcrm.auth.SecurityUtils;
import com.bdcrm.common.ApiException;
import com.bdcrm.organization.Organization;
import com.bdcrm.organization.OrganizationRepository;
import com.bdcrm.organization.OrganizationStatus;
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
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditEventService auditEventService;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        if (securityUtils.hasPlatformRole("PLATFORM_ADMIN")) {
            return userRepository.findAllByOrderByFullNameAsc().stream().map(UserResponse::from).toList();
        }
        return userRepository.findAllByOrganizationIdOrderByFullNameAsc(securityUtils.currentOrganizationId()).stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        Organization organization = resolveTargetOrganization(request.organizationId());
        if (userRepository.findByUsernameIgnoreCaseAndOrganizationId(request.username(), organization.getId()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Username already exists");
        }
        if (userRepository.findByEmailIgnoreCaseAndOrganizationId(request.email(), organization.getId()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already exists");
        }
        if (organization.getStatus() != com.bdcrm.organization.OrganizationStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Users can only be created in active organizations");
        }
        User user = new User();
        user.setUsername(request.username().trim());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setOrganization(organization);
        user.setActive(true);
        user.setRoles(new HashSet<>(loadRoles(request.roles())));
        if (request.managerId() != null) {
            User manager = requireUser(request.managerId());
            if (!manager.getOrganization().getId().equals(organization.getId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Manager must belong to the same organization");
            }
            user.setManager(manager);
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
        if (securityUtils.hasPlatformRole("PLATFORM_ADMIN")) {
            return userRepository.findById(userId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        }
        Long organizationId = securityUtils.currentOrganizationId();
        User user = userRepository.findByIdAndOrganizationId(userId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        return user;
    }

    private Organization resolveTargetOrganization(Long organizationId) {
        if (securityUtils.hasPlatformRole("PLATFORM_ADMIN")) {
            if (organizationId == null) {
                return securityUtils.currentOrganizationEntity();
            }
            return organizationRepository.findById(organizationId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Organization not found"));
        }
        if (organizationId != null && !organizationId.equals(securityUtils.currentOrganizationId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only create users in your own organization");
        }
        return securityUtils.currentOrganizationEntity();
    }

    private List<Role> loadRoles(java.util.Set<RoleName> roleNames) {
        if (!securityUtils.hasPlatformRole("PLATFORM_ADMIN") && roleNames.contains(RoleName.PLATFORM_ADMIN)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only platform admins can assign platform roles");
        }
        List<Role> roles = roleRepository.findByNameIn(roleNames);
        if (roles.size() != roleNames.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "One or more roles are invalid");
        }
        return roles;
    }
}
