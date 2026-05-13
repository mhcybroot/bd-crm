package com.bdcrm.audit;

import java.time.OffsetDateTime;

public record AuditEventResponse(
        Long id,
        String eventType,
        String entityType,
        Long entityId,
        String description,
        String detailsJson,
        Long actorUserId,
        String actorName,
        OffsetDateTime createdAt) {

    public static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getEventType(),
                event.getEntityType(),
                event.getEntityId(),
                event.getDescription(),
                event.getDetailsJson(),
                event.getActor() != null ? event.getActor().getId() : null,
                event.getActor() != null ? event.getActor().getFullName() : null,
                event.getCreatedAt());
    }
}
