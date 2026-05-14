package com.bdcrm.lead;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public final class NoteSpecifications {

    private NoteSpecifications() {
    }

    public static Specification<LeadNote> search(String search) {
        return (root, query, builder) -> {
            if (search == null || search.isBlank()) {
                return null;
            }
            String pattern = "%" + search.trim().toLowerCase() + "%";
            return builder.like(builder.lower(root.get("body")), pattern);
        };
    }

    public static Specification<LeadNote> forLead(Long leadId) {
        return (root, query, builder) -> leadId == null ? null : builder.equal(root.get("lead").get("id"), leadId);
    }
}