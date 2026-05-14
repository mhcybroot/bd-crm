package com.bdcrm.organization;

import com.bdcrm.auth.SecurityUtils;
import com.bdcrm.common.ApiException;
import com.bdcrm.user.RoleName;
import com.bdcrm.user.UserCreateRequest;
import com.bdcrm.user.UserResponse;
import com.bdcrm.user.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final SecurityUtils securityUtils;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<OrganizationResponse> listOrganizations() {
        return organizationRepository.findAllByOrderByNameAsc().stream()
                .map(OrganizationResponse::from)
                .toList();
    }

    @Transactional
    public OrganizationResponse createOrganization(OrganizationRequest request) {
        if (organizationRepository.findBySlugIgnoreCase(request.slug()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Organization slug already exists");
        }
        Organization organization = new Organization();
        apply(organization, request);
        return OrganizationResponse.from(organizationRepository.save(organization));
    }

    @Transactional
    public OrganizationBootstrapResponse bootstrapOrganization(OrganizationBootstrapRequest request) {
        if (request.organization().status() != null && request.organization().status() != OrganizationStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A bootstrap organization must be ACTIVE");
        }
        OrganizationResponse organization = createOrganization(request.organization());
        UserResponse adminUser = userService.createUser(new UserCreateRequest(
                request.adminUser().username(),
                request.adminUser().password(),
                request.adminUser().fullName(),
                request.adminUser().email(),
                null,
                organization.id(),
                java.util.Set.of(RoleName.ORG_ADMIN)));
        return new OrganizationBootstrapResponse(organization, adminUser);
    }

    @Transactional
    public OrganizationResponse updateOrganization(Long organizationId, OrganizationRequest request) {
        Organization organization = requireOrganization(organizationId);
        organizationRepository.findBySlugIgnoreCase(request.slug())
                .filter(existing -> !existing.getId().equals(organizationId))
                .ifPresent(existing -> {
                    throw new ApiException(HttpStatus.CONFLICT, "Organization slug already exists");
                });
        apply(organization, request);
        return OrganizationResponse.from(organization);
    }

    @Transactional(readOnly = true)
    public Organization requireActiveOrganization(Long organizationId) {
        Organization organization = requireOrganization(organizationId);
        if (organization.getStatus() != OrganizationStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Organization is not active");
        }
        return organization;
    }

    @Transactional(readOnly = true)
    public Organization currentOrganization() {
        return requireActiveOrganization(securityUtils.currentOrganizationId());
    }

    @Transactional(readOnly = true)
    public Organization requireOrganization(Long organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Organization not found"));
    }

    private void apply(Organization organization, OrganizationRequest request) {
        organization.setSlug(request.slug().trim().toLowerCase());
        organization.setName(request.name().trim());
        organization.setStatus(request.status() == null ? OrganizationStatus.ACTIVE : request.status());
        organization.setTimezone(request.timezone().trim());
        organization.setLocale(request.locale().trim());
        organization.setContactEmail(request.contactEmail().trim().toLowerCase());
        organization.setPlanCode(request.planCode().trim());
        organization.setDataRetentionDays(request.dataRetentionDays());
    }
}
