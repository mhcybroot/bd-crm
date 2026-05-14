package com.bdcrm.organization;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @GetMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public List<OrganizationResponse> listOrganizations() {
        return organizationService.listOrganizations();
    }

    @PostMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public OrganizationResponse createOrganization(@Valid @RequestBody OrganizationRequest request) {
        return organizationService.createOrganization(request);
    }

    @PostMapping("/bootstrap")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public OrganizationBootstrapResponse bootstrapOrganization(@Valid @RequestBody OrganizationBootstrapRequest request) {
        return organizationService.bootstrapOrganization(request);
    }

    @PutMapping("/{organizationId}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public OrganizationResponse updateOrganization(@PathVariable Long organizationId, @Valid @RequestBody OrganizationRequest request) {
        return organizationService.updateOrganization(organizationId, request);
    }
}
