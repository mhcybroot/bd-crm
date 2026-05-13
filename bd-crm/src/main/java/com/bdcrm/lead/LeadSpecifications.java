package com.bdcrm.lead;

import org.springframework.data.jpa.domain.Specification;

public final class LeadSpecifications {

    private LeadSpecifications() {
    }

    public static Specification<Lead> hasStatus(LeadStatus status) {
        return (root, query, builder) -> status == null ? null : builder.equal(root.get("status"), status);
    }

    public static Specification<Lead> assignedTo(Long userId) {
        return (root, query, builder) -> userId == null ? null : builder.equal(root.get("assignedUser").get("id"), userId);
    }

    public static Specification<Lead> search(String search) {
        return (root, query, builder) -> {
            if (search == null || search.isBlank()) {
                return null;
            }
            String pattern = "%" + search.trim().toLowerCase() + "%";
            return builder.or(
                    builder.like(builder.lower(root.get("companyName")), pattern),
                    builder.like(builder.lower(root.get("contactName")), pattern),
                    builder.like(builder.lower(root.get("email")), pattern),
                    builder.like(builder.lower(root.get("phone")), pattern),
                    builder.like(builder.lower(root.get("source")), pattern));
        };
    }
}
