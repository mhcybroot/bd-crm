package com.bdcrm.followup;

import org.springframework.data.jpa.domain.Specification;

public final class FollowupSpecifications {

    private FollowupSpecifications() {
    }

    public static Specification<LeadFollowup> search(String search) {
        return (root, query, builder) -> {
            if (search == null || search.isBlank()) {
                return null;
            }
            String pattern = "%" + search.trim().toLowerCase() + "%";
            return builder.like(builder.lower(root.get("notes")), pattern);
        };
    }

    public static Specification<LeadFollowup> hasOutcome(String outcome) {
        return (root, query, builder) -> outcome == null || outcome.isBlank()
                ? null
                : builder.equal(root.get("outcome").get("name"), outcome.toUpperCase());
    }

    public static Specification<LeadFollowup> forLead(Long leadId) {
        return (root, query, builder) -> leadId == null ? null : builder.equal(root.get("lead").get("id"), leadId);
    }
}