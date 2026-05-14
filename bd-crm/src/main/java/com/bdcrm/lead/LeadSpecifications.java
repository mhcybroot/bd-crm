package com.bdcrm.lead;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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

    public static Specification<Lead> organizationId(Long organizationId) {
        return (root, query, builder) -> organizationId == null ? null : builder.equal(root.get("organization").get("id"), organizationId);
    }

    public static Specification<Lead> template(Long templateId) {
        return (root, query, builder) -> templateId == null ? null : builder.equal(root.get("template").get("id"), templateId);
    }

    public static Specification<Lead> currentStage(Long stageId) {
        return (root, query, builder) -> stageId == null ? null : builder.equal(root.get("currentStage").get("id"), stageId);
    }

    public static Specification<Lead> priority(LeadPriority priority) {
        return (root, query, builder) -> priority == null ? null : builder.equal(root.get("priority"), priority);
    }

    public static Specification<Lead> source(String source) {
        return (root, query, builder) -> source == null || source.isBlank()
                ? null
                : builder.equal(builder.lower(root.get("source")), source.trim().toLowerCase());
    }

    public static Specification<Lead> createdBetween(LocalDate dateFrom, LocalDate dateTo) {
        return (root, query, builder) -> {
            if (dateFrom == null && dateTo == null) {
                return null;
            }
            Predicate predicate = builder.conjunction();
            if (dateFrom != null) {
                predicate = builder.and(predicate, builder.greaterThanOrEqualTo(
                        root.get("createdAt"),
                        dateFrom.atStartOfDay().atOffset(ZoneOffset.UTC)));
            }
            if (dateTo != null) {
                OffsetDateTime endOfDay = dateTo.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
                predicate = builder.and(predicate, builder.lessThan(root.get("createdAt"), endOfDay));
            }
            return predicate;
        };
    }

    public static Specification<Lead> notMerged() {
        return (root, query, builder) -> builder.isNull(root.get("mergedIntoLeadId"));
    }

    public static Specification<Lead> updatedBefore(OffsetDateTime timestamp) {
        return (root, query, builder) -> timestamp == null ? null : builder.lessThan(root.get("updatedAt"), timestamp);
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
