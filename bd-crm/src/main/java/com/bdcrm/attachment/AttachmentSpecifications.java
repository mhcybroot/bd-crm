package com.bdcrm.attachment;

import org.springframework.data.jpa.domain.Specification;

public final class AttachmentSpecifications {

    private AttachmentSpecifications() {
    }

    public static Specification<AttachmentRecord> search(String search) {
        return (root, query, builder) -> {
            if (search == null || search.isBlank()) {
                return null;
            }
            String pattern = "%" + search.trim().toLowerCase() + "%";
            return builder.like(builder.lower(root.get("originalFileName")), pattern);
        };
    }

    public static Specification<AttachmentRecord> forLead(Long leadId) {
        return (root, query, builder) -> leadId == null ? null : builder.equal(root.get("lead").get("id"), leadId);
    }
}