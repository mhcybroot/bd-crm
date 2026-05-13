package com.bdcrm.lead;

import java.time.OffsetDateTime;

public record LeadActivityResponse(
        Long id,
        LeadActivityType type,
        String description,
        Long actorId,
        String actorName,
        OffsetDateTime createdAt) {

    public static LeadActivityResponse from(LeadActivity activity) {
        return new LeadActivityResponse(
                activity.getId(),
                activity.getType(),
                activity.getDescription(),
                activity.getActor().getId(),
                activity.getActor().getFullName(),
                activity.getCreatedAt());
    }
}
