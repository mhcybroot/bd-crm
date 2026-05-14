package com.bdcrm.organization;

public record OrganizationResponse(
        Long id,
        String slug,
        String name,
        OrganizationStatus status,
        String timezone,
        String locale,
        String contactEmail,
        String planCode,
        int dataRetentionDays) {

    public static OrganizationResponse from(Organization organization) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getSlug(),
                organization.getName(),
                organization.getStatus(),
                organization.getTimezone(),
                organization.getLocale(),
                organization.getContactEmail(),
                organization.getPlanCode(),
                organization.getDataRetentionDays());
    }
}
