package com.bdcrm.lead;

import org.springframework.data.jpa.domain.Specification;

public final class ActivitySpecifications {

    private ActivitySpecifications() {
    }

    public static Specification<LeadActivity> search(String search) {
        return (root, query, builder) -> {
            if (search == null || search.isBlank()) {
                return null;
            }
            String pattern = "%" + search.trim().toLowerCase() + "%";
            return builder.like(builder.lower(root.get("description")), pattern);
        };
    }

    public static Specification<LeadActivity> forLead(Long leadId) {
        return (root, query, builder) -> leadId == null ? null : builder.equal(root.get("lead").get("id"), leadId);
    }
}