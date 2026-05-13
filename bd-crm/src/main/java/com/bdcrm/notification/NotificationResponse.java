package com.bdcrm.notification;

import java.time.OffsetDateTime;

public record NotificationResponse(
        Long id,
        String type,
        String title,
        String message,
        String actionUrl,
        Long leadId,
        Long followupId,
        OffsetDateTime readAt,
        OffsetDateTime createdAt) {

    public static NotificationResponse from(NotificationEvent event) {
        return new NotificationResponse(
                event.getId(),
                event.getType(),
                event.getTitle(),
                event.getMessage(),
                event.getActionUrl(),
                event.getLead() != null ? event.getLead().getId() : null,
                event.getFollowup() != null ? event.getFollowup().getId() : null,
                event.getReadAt(),
                event.getCreatedAt());
    }
}
